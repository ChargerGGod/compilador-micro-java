import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Interfaz extends JFrame {
    public Interfaz() {
        setTitle("Compilador Micro Java");
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(800, 600));
        JMenuBar menuBar = new JMenuBar();

        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem abrirItem = new JMenuItem("Abrir");
        JMenuItem guardarItem = new JMenuItem("Guardar");
        JMenuItem guardarComoItem = new JMenuItem("Guardar como");
        menuArchivo.add(abrirItem);
        menuArchivo.add(guardarItem);
        menuArchivo.add(guardarComoItem);

        JMenu menuVista = new JMenu("Vista");

        menuBar.add(menuArchivo);
        menuBar.add(menuVista);

        JButton tokenButton = new JButton("Token");
        tokenButton.setFocusable(false);
        JButton sintaxisButton = new JButton("Sintaxis");
        sintaxisButton.setFocusable(false);
        JButton semanticoButton = new JButton("Semántico"); // <-- Nuevo botón
        semanticoButton.setFocusable(false);

        menuBar.add(Box.createHorizontalStrut(10));
        menuBar.add(tokenButton);
        menuBar.add(sintaxisButton);
        menuBar.add(semanticoButton); // <-- Agrega el botón al menú

        setJMenuBar(menuBar);

        JTextArea textArea = new JTextArea();
        JScrollPane scrollTextArea = new JScrollPane(textArea);
        JPanel panelCodigo = new JPanel(new BorderLayout());
        JLabel labelCodigo = new JLabel("Código");
        labelCodigo.setHorizontalAlignment(SwingConstants.CENTER);
        panelCodigo.add(labelCodigo, BorderLayout.NORTH);
        panelCodigo.add(scrollTextArea, BorderLayout.CENTER);

        String[] columnas = {"Lexema", "Token"};
        DefaultTableModel tableModel = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(tableModel);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        JPanel panelTabla = new JPanel(new BorderLayout());
        JLabel labelTabla = new JLabel("Tabla de lexemas");
        labelTabla.setHorizontalAlignment(SwingConstants.CENTER);
        panelTabla.add(labelTabla, BorderLayout.NORTH);
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        JTextArea errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setBackground(new Color(240, 240, 240));
        JScrollPane scrollErrorArea = new JScrollPane(errorArea);
        JPanel panelErrores = new JPanel(new BorderLayout());
        JLabel labelErrores = new JLabel("Errores");
        labelErrores.setHorizontalAlignment(SwingConstants.CENTER);
        panelErrores.add(labelErrores, BorderLayout.NORTH);
        panelErrores.add(scrollErrorArea, BorderLayout.CENTER);

        JPanel panelTablaErrores = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTE = new GridBagConstraints();

        // Tabla de lexemas
        gbcTE.gridx = 0;
        gbcTE.gridy = 0;
        gbcTE.weightx = 1.0;
        gbcTE.weighty = 1.0;
        gbcTE.fill = GridBagConstraints.BOTH;
        gbcTE.insets = new Insets(0, 0, 0, 20); 
        panelTablaErrores.add(panelTabla, gbcTE);

        // Errores
        gbcTE.gridx = 1;
        gbcTE.insets = new Insets(0, 0, 0, 0); 
        panelTablaErrores.add(panelErrores, gbcTE);

        JPanel panelSuperior = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 2.0; // 2/3 para el área de texto
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panelSuperior.add(panelCodigo, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0; // 1/3 para el panel tabla+errores
        gbc.fill = GridBagConstraints.BOTH;
        panelSuperior.add(panelTablaErrores, gbc);

        JTextArea textAreaInferior1 = new JTextArea();
        JTextArea textAreaInferior2 = new JTextArea();
        JScrollPane scrollInferior1 = new JScrollPane(textAreaInferior1);
        JScrollPane scrollInferior2 = new JScrollPane(textAreaInferior2);

        JPanel panelInferior = new JPanel(new GridBagLayout());
        GridBagConstraints gbcInf = new GridBagConstraints();
        gbcInf.insets = new Insets(10, 10, 10, 10);

        gbcInf.gridx = 0;
        gbcInf.gridy = 0;
        gbcInf.weightx = 0.5;
        gbcInf.weighty = 1.0;
        gbcInf.fill = GridBagConstraints.BOTH;
        panelInferior.add(scrollInferior1, gbcInf);

        gbcInf.gridx = 1;
        panelInferior.add(scrollInferior2, gbcInf);

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSuperior, panelInferior);
        splitPrincipal.setResizeWeight(0.5); // Mitad y mitad
        splitPrincipal.setDividerLocation(0.5);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPrincipal, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPrincipal.setDividerLocation(0.5);
                panelSuperior.revalidate();
                panelInferior.revalidate();

                int width = panelSuperior.getWidth();
                int height = panelSuperior.getHeight();

                // El primer componente (área de texto) ocupa 2/3, el segundo (tabla+errores) 1/3
                scrollTextArea.setPreferredSize(new Dimension((int)(width * 2.0 / 3.0) - 15, height - 20));
                panelTablaErrores.setPreferredSize(new Dimension((int)(width * 1.0 / 3.0) - 15, height - 20));

                int rightWidth = panelTablaErrores.getWidth();
                scrollTabla.setPreferredSize(new Dimension((int)(rightWidth * 0.5), height - 20));
                scrollErrorArea.setPreferredSize(new Dimension((int)(rightWidth * 0.5), height - 20));

                panelSuperior.revalidate();
                panelTablaErrores.revalidate();
            }
        });

        final File[] archivoActual = {null};


        abrirItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Abrir archivo de texto");
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
                int resultado = fileChooser.showOpenDialog(Interfaz.this);
                if (resultado == JFileChooser.APPROVE_OPTION) {
                    File archivo = fileChooser.getSelectedFile();
                    try {
                        Path path = archivo.toPath();
                        String contenido = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
                        textArea.setText(contenido);
                        archivoActual[0] = archivo; // Guardar referencia al archivo abierto
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(Interfaz.this, "No se pudo abrir el archivo:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Acción para guardar archivo (solo sobrescribe si hay archivo abierto)
        guardarItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (archivoActual[0] != null) {
                    try {
                        Files.write(archivoActual[0].toPath(), textArea.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(Interfaz.this, "No se pudo guardar el archivo:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    guardarComoItem.doClick();
                }
            }
        });

        guardarComoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Guardar archivo de texto como");
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
                int resultado = fileChooser.showSaveDialog(Interfaz.this);
                if (resultado == JFileChooser.APPROVE_OPTION) {
                    java.io.File archivo = fileChooser.getSelectedFile();
                    // Asegura que el archivo tenga extensión .txt
                    if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                        archivo = new File(archivo.getParentFile(), archivo.getName() + ".txt");
                    }
                    try {
                        Files.write(archivo.toPath(), textArea.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        archivoActual[0] = archivo;
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(Interfaz.this, "No se pudo guardar el archivo:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Atajo de teclado para guardar: Ctrl + S 
        KeyStroke guardarKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        JRootPane rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(guardarKey, "guardarArchivo");
        rootPane.getActionMap().put("guardarArchivo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guardarItem.doClick();
            }
        });

        // Acción para el botón Token
        tokenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                errorArea.setText("");

                Lexico lexico = new Lexico();
                ArrayList<Token> tokens = lexico.Tokenizar(textArea.getText());

                String errores = lexico.getErrores();
                errorArea.setText(errores);

                if (errores.trim().isEmpty()) {
                    for (Token token : tokens) {
                        tableModel.addRow(new Object[]{token.getToken(), token.getLexema()});
                    }
                }
            }
        });

        // Acción para el botón Sintaxis
        sintaxisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorArea.setText("");
                Lexico lexico = new Lexico();
                ArrayList<Token> tokens = lexico.Tokenizar(textArea.getText());
                String erroresLexico = lexico.getErrores();

                if (!erroresLexico.trim().isEmpty()) {
                    errorArea.setText("Errores léxicos:\n" + erroresLexico);
                    return;
                }

                Sintactico sintactico = new Sintactico(new java.util.ArrayList<>(tokens));
                boolean correcto = sintactico.sintaxisCorrecta();

                if (correcto) {
                    errorArea.setText("Correcto");
                } else {
                    errorArea.setText("Error sintáctico");
                }
            }
        });

        // Acción para el botón Semántico
        semanticoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorArea.setText("");
                Lexico lexico = new Lexico();
                ArrayList<Token> tokens = lexico.Tokenizar(textArea.getText());
                String erroresLexico = lexico.getErrores();

                if (!erroresLexico.trim().isEmpty()) {
                    errorArea.setText("Errores léxicos:\n" + erroresLexico);
                    return;
                }

                Sintactico sintactico = new Sintactico(new java.util.ArrayList<>(tokens));
                boolean correcto = sintactico.sintaxisCorrecta();

                if (!correcto) {
                    errorArea.setText("Error sintáctico");
                    return;
                }

                Semantico semantico = new Semantico();
               semantico.analizar(tokens);
               boolean semanticoCorrecto = semantico.getErrores().isEmpty();
                if (semanticoCorrecto) {
                    errorArea.setText("Semántica correcta");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Errores semánticos:\n");
                        sb.append(semantico.getErrores());
                    errorArea.setText(sb.toString());
                }
            }
        });

        // Variables para controlar el tamaño de fuente
        final int[] fontSize = {14};
        final int fontSizeTabla = 16; 
        final int fontSizeTitulo = 18; 

        Runnable aplicarFuente = () -> {
            Font fuente = new Font(textArea.getFont().getName(), textArea.getFont().getStyle(), fontSize[0]);
            textArea.setFont(fuente);
            textAreaInferior1.setFont(fuente);
            textAreaInferior2.setFont(fuente);
            errorArea.setFont(fuente);

            Font fuenteTabla = new Font(tabla.getFont().getName(), tabla.getFont().getStyle(), fontSizeTabla);
            tabla.setFont(fuenteTabla);
            tabla.getTableHeader().setFont(fuenteTabla.deriveFont(Font.BOLD, fontSizeTabla));

            Font fuenteTitulo = new Font(labelCodigo.getFont().getName(), Font.BOLD, fontSizeTitulo);
            labelCodigo.setFont(fuenteTitulo);
            labelTabla.setFont(fuenteTitulo);
            labelErrores.setFont(fuenteTitulo);
        };
        aplicarFuente.run();

        // Atajos de teclado para aumentar/disminuir tamaño de letra: Ctrl + '+' y Ctrl + '-'
        KeyStroke aumentarKey = KeyStroke.getKeyStroke('+', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke aumentarKey2 = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()); // Para Ctrl + =
        KeyStroke disminuirKey = KeyStroke.getKeyStroke('-', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

        JRootPane rootPane2 = getRootPane();

        rootPane2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(aumentarKey, "aumentarLetra");
        rootPane2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(aumentarKey2, "aumentarLetra");
        rootPane2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(disminuirKey, "disminuirLetra");

        rootPane2.getActionMap().put("aumentarLetra", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fontSize[0] < 40) {
                    fontSize[0] += 2;
                    aplicarFuente.run();
                }
            }
        });

        rootPane2.getActionMap().put("disminuirLetra", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fontSize[0] > 8) {
                    fontSize[0] -= 2;
                    aplicarFuente.run();
                }
            }
        });

        setVisible(true);
    }
}
