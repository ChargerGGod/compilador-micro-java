import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PruebaInterfazCI extends JFrame {
    private JTextArea txtCodigoFuente;
    private JTextArea txtCodigoIntermedio;
    private JButton btnTraducir;

    public PruebaInterfazCI() {
        setTitle("Transformación a Código Intermedio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Pantalla completa
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(false);

        // Panel principal con GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Fuente grande
        Font fuenteGrande = new Font("Monospaced", Font.PLAIN, 22);

        // Cuadro de texto izquierdo (código fuente)
        txtCodigoFuente = new JTextArea();
        txtCodigoFuente.setFont(fuenteGrande);
        JScrollPane scrollFuente = new JScrollPane(txtCodigoFuente);
        scrollFuente.setBorder(BorderFactory.createTitledBorder("Código fuente"));

        // Cuadro de texto derecho (código intermedio)
        txtCodigoIntermedio = new JTextArea();
        txtCodigoIntermedio.setFont(fuenteGrande);
        txtCodigoIntermedio.setEditable(false);
        JScrollPane scrollIntermedio = new JScrollPane(txtCodigoIntermedio);
        scrollIntermedio.setBorder(BorderFactory.createTitledBorder("Código intermedio"));

        // Botón traducir
        btnTraducir = new JButton("Traducir →");
        btnTraducir.setFont(new Font("Arial", Font.BOLD, 22));
        btnTraducir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fuente = txtCodigoFuente.getText();
                String intermedio = PruebaCodigoIntermedio.traducirACodigoIntermedio(fuente);
                txtCodigoIntermedio.setText(intermedio);
            }
        });

        // Layout
        gbc.insets = new Insets(20, 20, 20, 20);

        // Cuadro fuente (izquierda)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollFuente, gbc);

        // Botón (arriba del cuadro derecho)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnTraducir, gbc);

        // Cuadro intermedio (derecha)
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollIntermedio, gbc);

        add(panel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PruebaInterfazCI());
    }
}