import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MainGUI extends JFrame {

    //Instâncias do Backend
    private Campeonato campeonato;
    private Grupo[]    grupos        = new Grupo[5];
    private int        totalGrupos   = 0;
    private Usuario[]  usuarios      = new Usuario[999];
    private int        totalUsuarios = 0;
    private Partida[]  partidas      = new Partida[100];
    private int        totalPartidas = 0;

    //Componentes de Interface
    private JTextField txtNomeCamp;
    private JButton btnCamp;
    private JComboBox<String> cbTimeMandante, cbTimeVisitante, cbGrupoIngresso;
    private JComboBox<String> cbUsuariosAposta, cbPartidasAposta, cbVencedorAposta;
    private JComboBox<String> cbPartidasEncerrar, cbGruposClassificacao;
    private JTextField txtGolsMandante, txtGolsVisitante, txtPlacarAposta;
    private JComboBox<String> cbExcluirTime, cbExcluirGrupo, cbExcluirUsuario, cbExcluirPartida;
    private JTextArea consoleArea;
    //inicia a interface
    public MainGUI() {
        super("Sistema de Bolão - Auto-Save Ativo");
        configurarInterface();
        redirecionarConsole();
        iniciarCarregamento();
    }
    //carrega o save do ultimo encerramenti di programa
    private void iniciarCarregamento() {

        carregarBackupTXT();

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> {
                consoleArea.setText("olá, programa pronto para utilização\n\n");


            });
        }).start();
    }

    //Logica de criamento do save
    //O save é composto pelo LOG de todas as ações feitas

    private void registrarLog(String linha) {
        try (FileWriter fw = new FileWriter("dados_bolao.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(linha);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void processarCriarCampeonato(String nome, boolean restaurando) {
        campeonato = new Campeonato(nome);

        txtNomeCamp.setText(nome);

        btnCamp.setEnabled(false);
        txtNomeCamp.setEnabled(false);

        if(!restaurando) {
            registrarLog("CAMP;" + nome);
            System.out.println("Campeonato '" + campeonato.getNome() + "' iniciado!");
        }
    }

    private void processarAdicionarTime(String nome, boolean restaurando) {
        if (campeonato == null) throw new IllegalStateException("Crie o campeonato primeiro!");
        if (campeonato.adicionarTime(nome)) {
            cbTimeMandante.addItem(nome); cbTimeVisitante.addItem(nome);
            if(cbExcluirTime != null) cbExcluirTime.addItem(nome);
            if(!restaurando) registrarLog("TIME;" + nome);
        }
    }

    private void processarCriarGrupo(String nome, boolean restaurando) {
        Grupo g = new Grupo(nome);
        grupos[totalGrupos++] = g;
        cbGrupoIngresso.addItem(g.getNome());
        cbGruposClassificacao.addItem(g.getNome());
        if(cbExcluirGrupo != null) cbExcluirGrupo.addItem(g.getNome());
        if(!restaurando) {
            registrarLog("GRUPO;" + nome);
            System.out.println("Grupo '" + nome + "' criado com sucesso!");
        }
    }

    private void processarCriarUsuario(String nome, int indexGrupo, boolean restaurando) {
        if (totalGrupos == 0) throw new IllegalStateException("Crie um grupo primeiro!");
        Usuario u = new Usuario(nome);
        usuarios[totalUsuarios++] = u;
        String labelUser = u.getNome() + " (ID: " + u.getId() + ")";
        cbUsuariosAposta.addItem(labelUser);
        if(cbExcluirUsuario != null) cbExcluirUsuario.addItem(labelUser);
        u.ingressar(grupos[indexGrupo]);
        if(!restaurando) registrarLog("USER;" + nome + ";" + indexGrupo);
    }

    private void processarAgendarPartida(String tm, String tv, String dataStr, String horaStr, boolean restaurando) {
        if (cbTimeMandante.getItemCount() < 2) throw new IllegalStateException("Adicione pelo menos 2 times!");
        LocalDate data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalTime hora = LocalTime.parse(horaStr, DateTimeFormatter.ofPattern("HH:mm"));
        Partida p = new Partida(campeonato, tm, tv, data, hora);
        partidas[totalPartidas++] = p;
        cbPartidasAposta.addItem(p.getNomePartida()); cbPartidasEncerrar.addItem(p.getNomePartida());
        if(cbExcluirPartida != null) cbExcluirPartida.addItem(p.getNomePartida());
        if(!restaurando) {
            registrarLog("PARTIDA;" + tm + ";" + tv + ";" + dataStr + ";" + horaStr);
            System.out.println("Partida " + p.getNomePartida() + " agendada!");
        }
    }

    private void processarRegistrarAposta(int idxUser, int idxPartida, String timeVencedor, String placar, boolean restaurando) {
        if (totalUsuarios == 0 || totalPartidas == 0) throw new IllegalStateException("Crie usuários e partidas!");
        Usuario u = usuarios[idxUser]; Partida p = partidas[idxPartida];
        if(!restaurando) System.out.println("\n--- Tentativa de Aposta ---");
        new Aposta(u, p, grupos, totalGrupos, timeVencedor, placar);
        if(!restaurando) registrarLog("APOSTA;" + idxUser + ";" + idxPartida + ";" + timeVencedor + ";" + placar);
    }

    private void processarEncerrarPartida(int idxPartida, int golsM, int golsV, boolean restaurando) {
        if (totalPartidas == 0) return;
        Partida p = partidas[idxPartida];
        if(!restaurando) System.out.println("\n--- Apito Final ---");
        p.registrarResultado(golsM, golsV);
        if(!restaurando) registrarLog("ENCERRAR;" + idxPartida + ";" + golsM + ";" + golsV);
    }

    private void processarDelCamp(boolean restaurando) {
        if (campeonato != null) {
            campeonato = null;
            btnCamp.setEnabled(true); txtNomeCamp.setEnabled(true); txtNomeCamp.setText("");
            cbTimeMandante.removeAllItems(); cbTimeVisitante.removeAllItems(); cbExcluirTime.removeAllItems();
            if(!restaurando) {
                System.out.println("\n[AVISO] Campeonato excluído. Os times foram resetados!");
                registrarLog("DEL_CAMP");
            }
        }
    }

    private void processarDelTime(String time, boolean restaurando) {
        campeonato.removerTime(time);
        cbTimeMandante.removeItem(time); cbTimeVisitante.removeItem(time); cbExcluirTime.removeItem(time);
        if(!restaurando) registrarLog("DEL_TIME;" + time);
    }

    private void processarDelGrupo(int idx, boolean restaurando) {
        String nome = grupos[idx].getNome();
        for (int i = idx; i < totalGrupos - 1; i++) grupos[i] = grupos[i + 1];
        grupos[totalGrupos - 1] = null; totalGrupos--;
        cbGrupoIngresso.removeItemAt(idx); cbGruposClassificacao.removeItemAt(idx); cbExcluirGrupo.removeItemAt(idx);
        if(!restaurando) {
            System.out.println("\n[AVISO] Grupo '" + nome + "' deletado definitivamente.");
            registrarLog("DEL_GRUPO;" + idx);
        }
    }

    private void processarDelUser(int idx, boolean restaurando) {
        String nomeInfo = usuarios[idx].getNome();
        for (int i = idx; i < totalUsuarios - 1; i++) usuarios[i] = usuarios[i + 1];
        usuarios[totalUsuarios - 1] = null; totalUsuarios--;
        cbUsuariosAposta.removeItemAt(idx); cbExcluirUsuario.removeItemAt(idx);
        if(!restaurando) {
            System.out.println("\n[AVISO] Usuário '" + nomeInfo + "' removido.");
            registrarLog("DEL_USER;" + idx);
        }
    }

    private void processarDelPartida(int idx, boolean restaurando) {
        Partida p = partidas[idx];
        if (p.isRealizada()) {
            if(!restaurando) throw new IllegalStateException("Não exclua uma partida já realizada!");
            return;
        }
        String nomePartida = p.getNomePartida();
        for (int i = idx; i < totalPartidas - 1; i++) partidas[i] = partidas[i + 1];
        partidas[totalPartidas - 1] = null; totalPartidas--;
        cbPartidasAposta.removeItemAt(idx); cbPartidasEncerrar.removeItemAt(idx); cbExcluirPartida.removeItemAt(idx);
        if(!restaurando) {
            System.out.println("\n[AVISO] Partida '" + nomePartida + "' removida.");
            registrarLog("DEL_PARTIDA;" + idx);
        }
    }

    //Responsavel por ler o ultimo save

    private void carregarBackupTXT() {
        File arquivo = new File("dados_bolao.txt");
        if (!arquivo.exists()) return;

        System.out.println("=========================================");
        System.out.println(" CARREGANDO O ARQUIVO dados_bolao.txt...");
        System.out.println("=========================================");
        try (Scanner scanner = new Scanner(arquivo)) {
            while (scanner.hasNextLine()) {
                String linha = scanner.nextLine().trim();
                if (linha.isEmpty()) continue;
                String[] partes = linha.split(";");
                try {
                    switch (partes[0]) {
                        case "CAMP": processarCriarCampeonato(partes[1], true); break;
                        case "TIME": processarAdicionarTime(partes[1], true); break;
                        case "GRUPO": processarCriarGrupo(partes[1], true); break;
                        case "USER": processarCriarUsuario(partes[1], Integer.parseInt(partes[2]), true); break;
                        case "PARTIDA": processarAgendarPartida(partes[1], partes[2], partes[3], partes[4], true); break;
                        case "APOSTA": processarRegistrarAposta(Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), partes[3], partes[4], true); break;
                        case "ENCERRAR": processarEncerrarPartida(Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3]), true); break;
                        case "DEL_CAMP": processarDelCamp(true); break;
                        case "DEL_TIME": processarDelTime(partes[1], true); break;
                        case "DEL_GRUPO": processarDelGrupo(Integer.parseInt(partes[1]), true); break;
                        case "DEL_USER": processarDelUser(Integer.parseInt(partes[1]), true); break;
                        case "DEL_PARTIDA": processarDelPartida(Integer.parseInt(partes[1]), true); break;
                    }
                } catch (Exception ignored) {} // Ignora falhas de linhas corrompidas e continua
            }
            System.out.println("=========================================");
            System.out.println(" DADOS RESTAURADOS COM SUCESSO!");
            System.out.println("=========================================\n");
        } catch (Exception e) { System.out.println("Erro ao ler backup."); }
    }

    //Montagem da interface

    private void configurarInterface() {
        setSize(950, 800); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setLocationRelativeTo(null); setLayout(new BorderLayout(10, 10));
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("1. Cadastros Básicos", criarPainelCadastros());
        tabbedPane.addTab("2. Fazer Apostas", criarPainelApostas());
        tabbedPane.addTab("3. Painel de Controle", criarPainelControle());
        tabbedPane.addTab("4. Gerenciar Exclusões", criarPainelExclusoes());
        add(tabbedPane, BorderLayout.NORTH);

        consoleArea = new JTextArea(); consoleArea.setEditable(false); consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        consoleArea.setBackground(new Color(30, 30, 30)); consoleArea.setForeground(new Color(0, 255, 0));
        JScrollPane scrollConsole = new JScrollPane(consoleArea); scrollConsole.setBorder(BorderFactory.createTitledBorder("Console"));
        add(scrollConsole, BorderLayout.CENTER);
    }

    private JScrollPane criarPainelCadastros() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel pCamp = criarSecao("1. Criar Campeonato"); txtNomeCamp = novoTextField(20); btnCamp = new JButton("Criar");
        btnCamp.addActionListener(e -> { try { processarCriarCampeonato(txtNomeCamp.getText().trim(), false); } catch(Exception ex) { mostrarErro(ex.getMessage()); } });
        pCamp.add(new JLabel("Nome:")); pCamp.add(txtNomeCamp); pCamp.add(btnCamp);

        JPanel pTime = criarSecao("2. Adicionar Time"); JTextField txtNomeTime = novoTextField(20); JButton btnTime = new JButton("Adicionar Time");
        btnTime.addActionListener(e -> { try { processarAdicionarTime(txtNomeTime.getText().trim(), false); txtNomeTime.setText(""); } catch(Exception ex) { mostrarErro(ex.getMessage()); } });
        pTime.add(new JLabel("Nome:")); pTime.add(txtNomeTime); pTime.add(btnTime);

        JPanel pGrupo = criarSecao("3. Criar Grupo de Apostas"); JTextField txtNomeGrupo = novoTextField(20); JButton btnGrupo = new JButton("Criar Grupo");
        btnGrupo.addActionListener(e -> { try { processarCriarGrupo(txtNomeGrupo.getText().trim(), false); txtNomeGrupo.setText(""); } catch(Exception ex) { mostrarErro(ex.getMessage()); } });
        pGrupo.add(new JLabel("Nome:")); pGrupo.add(txtNomeGrupo); pGrupo.add(btnGrupo);

        JPanel pUser = criarSecao("4. Criar Usuário"); JTextField txtNomeUser = novoTextField(15); cbGrupoIngresso = novoComboBox(); JButton btnUser = new JButton("Criar e Ingressar");
        btnUser.addActionListener(e -> { try { processarCriarUsuario(txtNomeUser.getText().trim(), cbGrupoIngresso.getSelectedIndex(), false); txtNomeUser.setText(""); } catch(Exception ex) { mostrarErro(ex.getMessage()); } });
        pUser.add(new JLabel("Nome:")); pUser.add(txtNomeUser); pUser.add(new JLabel("  Entrar no Grupo:")); pUser.add(cbGrupoIngresso); pUser.add(btnUser);

        JPanel pPartida = criarSecao("5. Agendar Partida"); cbTimeMandante = novoComboBox(); cbTimeVisitante = novoComboBox();
        JTextField txtData = novoTextField(8); txtData.setText("20/12/2026"); JTextField txtHora = novoTextField(5); txtHora.setText("16:00"); JButton btnPartida = new JButton("Agendar");
        btnPartida.addActionListener(e -> { try { processarAgendarPartida((String)cbTimeMandante.getSelectedItem(), (String)cbTimeVisitante.getSelectedItem(), txtData.getText(), txtHora.getText(), false); } catch(Exception ex) { mostrarErro(ex.getMessage()); } });
        pPartida.add(cbTimeMandante); pPartida.add(new JLabel(" X ")); pPartida.add(cbTimeVisitante); pPartida.add(new JLabel("   Data:")); pPartida.add(txtData); pPartida.add(new JLabel(" Hora:")); pPartida.add(txtHora); pPartida.add(btnPartida);

        panel.add(pCamp); panel.add(Box.createVerticalStrut(10)); panel.add(pTime); panel.add(Box.createVerticalStrut(10));
        panel.add(pGrupo); panel.add(Box.createVerticalStrut(10)); panel.add(pUser); panel.add(Box.createVerticalStrut(10)); panel.add(pPartida);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.add(panel, BorderLayout.NORTH); JScrollPane scroll = new JScrollPane(wrapper); scroll.setBorder(null); return scroll;
    }

    private JPanel criarPainelApostas() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 15, 15)); panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Usuário:")); cbUsuariosAposta = novoComboBox(); panel.add(cbUsuariosAposta);
        panel.add(new JLabel("Partida:")); cbPartidasAposta = novoComboBox(); panel.add(cbPartidasAposta);
        panel.add(new JLabel("Palpite Placar (ex: 2x1):")); txtPlacarAposta = novoTextField(10); panel.add(txtPlacarAposta);
        panel.add(new JLabel("Vencedor:")); cbVencedorAposta = novoComboBox(); cbVencedorAposta.addItem("Mandante"); cbVencedorAposta.addItem("Visitante"); cbVencedorAposta.addItem("Empate"); panel.add(cbVencedorAposta);

        JButton btnApostar = new JButton("Registrar Aposta");
        btnApostar.addActionListener(e -> {
            try {
                String esc = (String) cbVencedorAposta.getSelectedItem(); Partida p = partidas[cbPartidasAposta.getSelectedIndex()];
                String timeVencedor = esc.equals("Mandante") ? p.getTimeMandante() : esc.equals("Visitante") ? p.getTimeVisitante() : "empate";
                processarRegistrarAposta(cbUsuariosAposta.getSelectedIndex(), cbPartidasAposta.getSelectedIndex(), timeVencedor, txtPlacarAposta.getText().trim(), false); txtPlacarAposta.setText("");
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });
        panel.add(new JLabel()); panel.add(btnApostar);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.add(panel, BorderLayout.NORTH); return wrapper;
    }

    private JPanel criarPainelControle() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JPanel pEncerrar = new JPanel(new GridLayout(4, 2, 10, 10)); pEncerrar.setBorder(BorderFactory.createTitledBorder("1. Encerrar Partida"));
        pEncerrar.add(new JLabel("Partida:")); cbPartidasEncerrar = novoComboBox(); pEncerrar.add(cbPartidasEncerrar);
        pEncerrar.add(new JLabel("Gols Mandante:")); txtGolsMandante = novoTextField(5); pEncerrar.add(txtGolsMandante);
        pEncerrar.add(new JLabel("Gols Visitante:")); txtGolsVisitante = novoTextField(5); pEncerrar.add(txtGolsVisitante);

        pEncerrar.add(new JLabel()); JButton btnEncerrar = new JButton("Gravar Resultado");
        btnEncerrar.addActionListener(e -> {
            try { processarEncerrarPartida(cbPartidasEncerrar.getSelectedIndex(), Integer.parseInt(txtGolsMandante.getText().trim()), Integer.parseInt(txtGolsVisitante.getText().trim()), false); txtGolsMandante.setText(""); txtGolsVisitante.setText(""); } catch (Exception ex) { mostrarErro("Verifique os gols."); }
        }); pEncerrar.add(btnEncerrar);

        JPanel pRanking = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15)); pRanking.setBorder(BorderFactory.createTitledBorder("2. Ranking e Pontuações"));
        pRanking.add(new JLabel("Ranking do Grupo:")); cbGruposClassificacao = novoComboBox(); pRanking.add(cbGruposClassificacao);
        JButton btnRanking = new JButton("Exibir Classificação"); btnRanking.addActionListener(e -> { if (totalGrupos>0) {System.out.println("\n"); grupos[cbGruposClassificacao.getSelectedIndex()].classificacao();} }); pRanking.add(btnRanking);

        panel.add(pEncerrar); panel.add(Box.createVerticalStrut(20)); panel.add(pRanking);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.add(panel, BorderLayout.NORTH); return wrapper;
    }

    private JScrollPane criarPainelExclusoes() {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel pCamp = criarSecao("1. Destruir Campeonato Atual"); JButton btnDelCamp = new JButton("Excluir Campeonato"); btnDelCamp.setForeground(Color.RED);
        btnDelCamp.addActionListener(e -> { try { processarDelCamp(false); } catch (Exception ex) { mostrarErro(ex.getMessage()); } }); pCamp.add(btnDelCamp);

        JPanel pTime = criarSecao("2. Excluir Time"); cbExcluirTime = novoComboBox(); JButton btnDelTime = new JButton("Excluir Time");
        btnDelTime.addActionListener(e -> { try { if(cbExcluirTime.getItemCount()>0) processarDelTime((String)cbExcluirTime.getSelectedItem(), false); } catch (Exception ex) { mostrarErro(ex.getMessage()); } });
        pTime.add(new JLabel("Selecione:")); pTime.add(cbExcluirTime); pTime.add(btnDelTime);

        JPanel pGrupo = criarSecao("3. Excluir Grupo"); cbExcluirGrupo = novoComboBox(); JButton btnDelGrupo = new JButton("Excluir Grupo");
        btnDelGrupo.addActionListener(e -> { try { if(cbExcluirGrupo.getSelectedIndex()>=0) processarDelGrupo(cbExcluirGrupo.getSelectedIndex(), false); } catch (Exception ex) { mostrarErro(ex.getMessage()); } });
        pGrupo.add(new JLabel("Selecione:")); pGrupo.add(cbExcluirGrupo); pGrupo.add(btnDelGrupo);

        JPanel pUser = criarSecao("4. Excluir Usuário"); cbExcluirUsuario = novoComboBox(); JButton btnDelUser = new JButton("Excluir Usuário");
        btnDelUser.addActionListener(e -> { try { if(cbExcluirUsuario.getSelectedIndex()>=0) processarDelUser(cbExcluirUsuario.getSelectedIndex(), false); } catch (Exception ex) { mostrarErro(ex.getMessage()); } });
        pUser.add(new JLabel("Selecione:")); pUser.add(cbExcluirUsuario); pUser.add(btnDelUser);

        JPanel pPartida = criarSecao("5. Excluir Partida"); cbExcluirPartida = novoComboBox(); JButton btnDelPartida = new JButton("Excluir Partida");
        btnDelPartida.addActionListener(e -> { try { if(cbExcluirPartida.getSelectedIndex()>=0) processarDelPartida(cbExcluirPartida.getSelectedIndex(), false); } catch (Exception ex) { mostrarErro(ex.getMessage()); } });
        pPartida.add(new JLabel("Selecione:")); pPartida.add(cbExcluirPartida); pPartida.add(btnDelPartida);

        panel.add(pCamp); panel.add(Box.createVerticalStrut(10)); panel.add(pTime); panel.add(Box.createVerticalStrut(10));
        panel.add(pGrupo); panel.add(Box.createVerticalStrut(10)); panel.add(pUser); panel.add(Box.createVerticalStrut(10)); panel.add(pPartida);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.add(panel, BorderLayout.NORTH); JScrollPane scroll = new JScrollPane(wrapper); scroll.setBorder(null); return scroll;
    }

    private JPanel criarSecao(String titulo) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); p.setBorder(BorderFactory.createTitledBorder(titulo)); return p; }
    private JTextField novoTextField(int c) { JTextField txt = new JTextField(c); txt.setPreferredSize(new Dimension(txt.getPreferredSize().width, 30)); return txt; }
    private JComboBox<String> novoComboBox() { JComboBox<String> cb = new JComboBox<>(); cb.setPreferredSize(new Dimension(180, 30)); return cb; }
    private void mostrarErro(String msg) { JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE); }

    private void redirecionarConsole() {
        OutputStream out = new OutputStream() {
            @Override public void write(int b) { consoleArea.append(String.valueOf((char) b)); }
            @Override public void write(byte[] b, int off, int len) {
                consoleArea.append(new String(b, off, len, java.nio.charset.StandardCharsets.UTF_8));
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            }
        };
        try { System.setOut(new PrintStream(out, true, "UTF-8")); System.setErr(new PrintStream(out, true, "UTF-8")); }
        catch (Exception e) { System.setOut(new PrintStream(out, true)); System.setErr(new PrintStream(out, true)); }
    }

    public static void main(String[] args) {
        Font fonte = new Font("SansSerif", Font.PLAIN, 14); UIManager.put("Label.font", fonte); UIManager.put("TextField.font", fonte); UIManager.put("ComboBox.font", fonte); UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 13)); UIManager.put("TitledBorder.font", new Font("SansSerif", Font.BOLD, 14));
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
    }
}