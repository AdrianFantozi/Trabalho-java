import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

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

    private void iniciarCarregamento() {

        carregarDadosDoBanco();

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
            SwingUtilities.invokeLater(() -> {
                consoleArea.setText("olá, programa pronto para utilização com H2 Database\n\n");
            });
        }).start();
    }


    private void processarCriarCampeonato(String nome, boolean restaurando) {
        campeonato = new Campeonato(nome);

        txtNomeCamp.setText(nome);

        btnCamp.setEnabled(false);
        txtNomeCamp.setEnabled(false);

        if(!restaurando) {
            salvarCampeonatoNoBanco(nome);
            System.out.println("Campeonato '" + campeonato.getNome() + "' iniciado!");
        }
    }

    private void processarAdicionarTime(String nome, boolean restaurando) {
        if (campeonato == null) throw new IllegalStateException("Crie o campeonato primeiro!");
        if (campeonato.adicionarTime(nome)) {
            cbTimeMandante.addItem(nome); cbTimeVisitante.addItem(nome);
            if(cbExcluirTime != null) cbExcluirTime.addItem(nome);
            if(!restaurando) {
                salvarTimeNoBanco(nome);
            }
        }
    }

    private void processarCriarGrupo(String nome, boolean restaurando) {
        Grupo g = new Grupo(nome);
        grupos[totalGrupos++] = g;
        cbGrupoIngresso.addItem(g.getNome());
        cbGruposClassificacao.addItem(g.getNome());
        if(cbExcluirGrupo != null) cbExcluirGrupo.addItem(g.getNome());

        if(!restaurando) {
            salvarGrupoNoBanco(g);
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
        if(!restaurando) {
            salvarUsuarioNoBanco(u);
            salvarMembroGrupoNoBanco(grupos[indexGrupo].getNome(), u.getId());
        }
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
            salvarPartidaNoBanco(p);
            System.out.println("Partida " + p.getNomePartida() + " agendada!");
        }
    }

    private void processarRegistrarAposta(int idxUser, int idxPartida, String timeVencedor, String placar, boolean restaurando) {
        if (totalUsuarios == 0 || totalPartidas == 0) throw new IllegalStateException("Crie usuários e partidas!");

        Usuario u = usuarios[idxUser];
        Partida p = partidas[idxPartida];
        if(!restaurando) System.out.println("\n--- Tentativa de Aposta ---");
        Aposta novaAposta = new Aposta(u, p, grupos, totalGrupos, timeVencedor, placar);
        if(!restaurando) {
            salvarApostaNoBanco(novaAposta);
        }
    }

    private void processarEncerrarPartida(int idxPartida, int golsM, int golsV, boolean restaurando) {
        if (totalPartidas == 0) return;
        Partida p = partidas[idxPartida];
        if(!restaurando) System.out.println("\n--- Apito Final ---");
        p.registrarResultado(golsM, golsV);
        if(!restaurando) {
            atualizarPartidaNoBanco(p);
            for (int i = 0; i < p.getTotalApostas(); i++) {
                Aposta a = p.getApostas()[i];
                atualizarApostaNoBanco(a);
                atualizarPontuacaoUsuarioNoBanco(a.getUsuario());
            }
        }
    }

    private void processarDelCamp(boolean restaurando) {
        if (campeonato != null) {
            campeonato = null;
            btnCamp.setEnabled(true); txtNomeCamp.setEnabled(true); txtNomeCamp.setText("");
            cbTimeMandante.removeAllItems(); cbTimeVisitante.removeAllItems(); cbExcluirTime.removeAllItems();
            if(!restaurando) {
                deletarCampeonatoDoBanco();
                System.out.println("\n[AVISO] Campeonato excluído. Os times foram resetados!");
            }
        }
    }

    private void processarDelTime(String time, boolean restaurando) {
        campeonato.removerTime(time);
        cbTimeMandante.removeItem(time); cbTimeVisitante.removeItem(time); cbExcluirTime.removeItem(time);
        if(!restaurando) {
            deletarTimeDoBanco(time);
        }
    }

    private void processarDelGrupo(int idx, boolean restaurando) {
        String nome = grupos[idx].getNome();
        for (int i = idx; i < totalGrupos - 1; i++) grupos[i] = grupos[i + 1];
        grupos[totalGrupos - 1] = null; totalGrupos--;
        cbGrupoIngresso.removeItemAt(idx); cbGruposClassificacao.removeItemAt(idx); cbExcluirGrupo.removeItemAt(idx);
        if(!restaurando) {
            deletarGrupoDoBanco(nome);
            System.out.println("\n[AVISO] Grupo '" + nome + "' deletado definitivamente.");
        }
    }

    private void processarDelUser(int idx, boolean restaurando) {
        String idParaDeletar = usuarios[idx].getId();
        String nomeInfo = usuarios[idx].getNome();

        for (int i = idx; i < totalUsuarios - 1; i++) usuarios[i] = usuarios[i + 1];
        usuarios[totalUsuarios - 1] = null; totalUsuarios--;
        cbUsuariosAposta.removeItemAt(idx); cbExcluirUsuario.removeItemAt(idx);

        if(!restaurando) {
            deletarUsuarioDoBanco(idParaDeletar);
            System.out.println("\n[AVISO] Usuário '" + nomeInfo + "' removido.");
        }
    }

    private void processarDelPartida(int idx, boolean restaurando) {
        Partida p = partidas[idx];
        if (p.isRealizada()) {
            if(!restaurando) throw new IllegalStateException("Não exclua uma partida já realizada!");
            return;
        }

        String idParaDeletar = p.getId();
        String nomePartida = p.getNomePartida();

        for (int i = idx; i < totalPartidas - 1; i++) partidas[i] = partidas[i + 1];
        partidas[totalPartidas - 1] = null; totalPartidas--;
        cbPartidasAposta.removeItemAt(idx); cbPartidasEncerrar.removeItemAt(idx); cbExcluirPartida.removeItemAt(idx);

        if(!restaurando) {
            deletarPartidaDoBanco(idParaDeletar);
            System.out.println("\n[AVISO] Partida '" + nomePartida + "' removida.");
        }
    }

    //Responsavel por ler o ultimo save

    private void carregarDadosDoBanco() {
        System.out.println("=========================================");
        System.out.println(" CARREGANDO DADOS DO H2 DATABASE...");
        System.out.println("=========================================");

        carregarGruposDoBanco();
        carregarUsuariosDoBanco();
        carregarPartidasDoBanco();
        carregarApostasDoBanco();

        System.out.println("=========================================");
        System.out.println(" BANCO DE DADOS SINCRONIZADO! ");
        System.out.println("=========================================\n");
    }


    private void carregarGruposDoBanco() {
        String sql = "SELECT nome FROM grupos";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nome = rs.getString("nome");
                // Usamos o 'true' para que ele não tente salvar no banco novamente
                processarCriarGrupo(nome, true);
            }
        } catch (SQLException e) { System.err.println("Erro carregar grupos: " + e.getMessage()); }
    }

    private void carregarUsuariosDoBanco() {
        String sql = "SELECT u.id, u.nome, u.pontuacao, m.grupo_nome " +
                "FROM usuarios u " +
                "LEFT JOIN membros_grupo m ON u.id = m.usuario_id";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String nome = rs.getString("nome");
                int pontuacao = rs.getInt("pontuacao");
                String nomeGrupo = rs.getString("grupo_nome");

                int indexGrupo = 0;
                for (int i = 0; i < totalGrupos; i++) {
                    if (grupos[i].getNome().equals(nomeGrupo)) {
                        indexGrupo = i;
                        break;
                    }
                }

                processarCriarUsuario(nome, indexGrupo, true);


                Usuario u = usuarios[totalUsuarios - 1];
                u.setId(id);
                u.setPontuacao(pontuacao);
            }
        } catch (SQLException e) { System.err.println("Erro carregar usuários: " + e.getMessage()); }
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

        panel.add(new JLabel());
        panel.add(criarBotaoApostar());
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
        JButton btnRanking = new JButton("Exibir Classificação");
        btnRanking.addActionListener(e -> {
            if (totalGrupos > 0) {
                consoleArea.setText(""); // Limpa o console antes de exibir
                exibirClassificacaoDoBanco((String) cbGruposClassificacao.getSelectedItem());
            }
        });
        pRanking.add(btnRanking);

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

    private void salvarUsuarioNoBanco(Usuario u) {
        String sql = "INSERT INTO usuarios (id, nome, pontuacao) VALUES (?, ?, ?)";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, u.getId());
            stmt.setString(2, u.getNome());
            stmt.setInt(3, u.getPontuacao());

            stmt.executeUpdate();
            System.out.println("-> Usuário '" + u.getNome() + "' salvo no banco de dados H2 com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao salvar usuário no banco: " + e.getMessage());
        }
    }

    private void salvarPartidaNoBanco(Partida p) {
        String sql = "INSERT INTO partidas (id, time_mandante, time_visitante, data_partida, horario, gols_mandante, gols_visitante, realizada) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getId());
            stmt.setString(2, p.getTimeMandante());
            stmt.setString(3, p.getTimeVisitante());
            stmt.setObject(4, p.getData());
            stmt.setObject(5, p.getHorario());
            stmt.setInt(6, p.getGolsMandante());
            stmt.setInt(7, p.getGolsVisitante());
            stmt.setBoolean(8, p.isRealizada());

            stmt.executeUpdate();
            System.out.println("-> Partida '" + p.getNomePartida() + "' salva no banco de dados H2!");

        } catch (SQLException e) {
            System.err.println("Erro ao salvar partida no banco: " + e.getMessage());
        }
    }

    private void atualizarPartidaNoBanco(Partida p) {
        String sql = "UPDATE partidas SET gols_mandante = ?, gols_visitante = ?, realizada = ? WHERE id = ?";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, p.getGolsMandante());
            stmt.setInt(2, p.getGolsVisitante());
            stmt.setBoolean(3, p.isRealizada());
            stmt.setString(4, p.getId());

            stmt.executeUpdate();
            System.out.println("-> Resultado da partida '" + p.getNomePartida() + "' atualizado no H2 com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar partida no banco: " + e.getMessage());
        }
    }

    private void salvarApostaNoBanco(Aposta a) {
        String sql = "INSERT INTO apostas (id, usuario_id, partida_id, time_ganhador, resultado, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, a.getId());
            stmt.setString(2, a.getUsuario().getId());
            stmt.setString(3, a.getPartida().getId());
            stmt.setString(4, a.getTimeGanhador());
            stmt.setString(5, a.getResultado());
            stmt.setString(6, a.getStatus().name());

            stmt.executeUpdate();
            System.out.println("-> Aposta '" + a.getId() + "' salva no banco de dados H2!");

        } catch (SQLException e) {
            System.err.println("Erro ao salvar a aposta no banco: " + e.getMessage());
        }
    }

    private void atualizarApostaNoBanco(Aposta a) {
        String sql = "UPDATE apostas SET status = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, a.getStatus().name());
            stmt.setString(2, a.getId());
            stmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Erro atualizar aposta: " + e.getMessage()); }
    }

    private void atualizarPontuacaoUsuarioNoBanco(Usuario u) {
        String sql = "UPDATE usuarios SET pontuacao = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, u.getPontuacao());
            stmt.setString(2, u.getId());
            stmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Erro atualizar usuário: " + e.getMessage()); }
    }

    private void salvarGrupoNoBanco(Grupo g) {
        String sql = "INSERT INTO grupos (nome) VALUES (?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, g.getNome());
            stmt.executeUpdate();
            System.out.println("-> Grupo '" + g.getNome() + "' salvo no banco de dados H2!");
        } catch (SQLException e) { System.err.println("Erro ao salvar grupo: " + e.getMessage()); }
    }

    private void salvarMembroGrupoNoBanco(String nomeGrupo, String usuarioId) {
        String sql = "INSERT INTO membros_grupo (grupo_nome, usuario_id, pontos_no_grupo) VALUES (?, ?, 0)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nomeGrupo);
            stmt.setString(2, usuarioId);
            stmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Erro ao vincular membro: " + e.getMessage()); }
    }

    private void carregarPartidasDoBanco() {
        String sql = "SELECT * FROM partidas";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String tm = rs.getString("time_mandante");
                String tv = rs.getString("time_visitante");
                java.sql.Date dataSql = rs.getDate("data_partida");
                java.sql.Time horaSql = rs.getTime("horario");
                int golsM = rs.getInt("gols_mandante");
                int golsV = rs.getInt("gols_visitante");
                boolean realizada = rs.getBoolean("realizada");

                if (campeonato == null) processarCriarCampeonato("Campeonato Bolão", true);
                if (!campeonato.timeExiste(tm)) processarAdicionarTime(tm, true);
                if (!campeonato.timeExiste(tv)) processarAdicionarTime(tv, true);

                // Converte os formatos do banco para String para usar no método da interface
                String dataStr = dataSql.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String horaStr = horaSql.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

                // Cria a partida na interface
                processarAgendarPartida(tm, tv, dataStr, horaStr, true);

                // Ajusta os dados da partida para ficarem idênticos ao banco
                Partida p = partidas[totalPartidas - 1];
                p.setId(id);
                if (realizada) {
                    p.setGolsMandante(golsM);
                    p.setGolsVisitante(golsV);
                    p.setRealizada(true);
                }
            }
        } catch (SQLException e) { System.err.println("Erro carregar partidas: " + e.getMessage()); }
    }

    private void carregarApostasDoBanco() {
        String sql = "SELECT * FROM apostas";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String usuarioId = rs.getString("usuario_id");
                String partidaId = rs.getString("partida_id");
                String timeGanhador = rs.getString("time_ganhador");
                String resultado = rs.getString("resultado");
                String statusNome = rs.getString("status");

                int idxUser = -1, idxPartida = -1;
                for (int i = 0; i < totalUsuarios; i++) {
                    if (usuarios[i].getId().equals(usuarioId)) { idxUser = i; break; }
                }
                for (int i = 0; i < totalPartidas; i++) {
                    if (partidas[i].getId().equals(partidaId)) { idxPartida = i; break; }
                }

                if (idxUser != -1 && idxPartida != -1) {
                    processarRegistrarAposta(idxUser, idxPartida, timeGanhador, resultado, true);

                    Partida p = partidas[idxPartida];
                    Aposta a = p.getApostas()[p.getTotalApostas() - 1];
                    a.setId(id);
                    a.setStatus(Aposta.Status.valueOf(statusNome));
                }
            }
        } catch (SQLException e) { System.err.println("Erro carregar apostas: " + e.getMessage()); }
    }

        private void salvarCampeonatoNoBanco(String nome) {
            String sql = "MERGE INTO campeonatos (nome) VALUES (?)";
            try (Connection conn = ConexaoDB.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nome);
                stmt.executeUpdate();
            } catch (SQLException e) { System.err.println("Erro ao salvar campeonato: " + e.getMessage()); }
        }

    private void salvarTimeNoBanco(String nome) {
        String sql = "MERGE INTO times (nome) VALUES (?)";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Erro ao salvar time: " + e.getMessage()); }
    }

    private void deletarCampeonatoDoBanco() {
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmtCamp = conn.prepareStatement("DELETE FROM campeonatos");
             PreparedStatement stmtTimes = conn.prepareStatement("DELETE FROM times")) {
            stmtTimes.executeUpdate();
            stmtCamp.executeUpdate();
        } catch (SQLException e) { System.err.println("Erro ao deletar campeonato: " + e.getMessage()); }
    }

    private void deletarTimeDoBanco(String nome) {
        String sql = "DELETE FROM times WHERE nome = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Erro ao deletar time: " + e.getMessage()); }
    }

    private void deletarGrupoDoBanco(String nome) {
        String sql = "DELETE FROM grupos WHERE nome = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.executeUpdate();

        } catch (SQLException e) { System.err.println("Erro ao deletar grupo: " + e.getMessage()); }
    }

    private void deletarUsuarioDoBanco(String id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) { System.err.println("Erro ao deletar usuário: " + e.getMessage()); }
    }

    private void deletarPartidaDoBanco(String id) {
        String sql = "DELETE FROM partidas WHERE id = ?";
        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) { System.err.println("Erro ao deletar partida: " + e.getMessage()); }
    }

    private JButton criarBotaoApostar() {
        JButton btnApostar = new JButton("Registrar Aposta");
        btnApostar.addActionListener(e -> {
            try {
                String esc = (String) cbVencedorAposta.getSelectedItem();
                Partida p = partidas[cbPartidasAposta.getSelectedIndex()];
                String timeVencedor = esc.equals("Mandante") ? p.getTimeMandante() : esc.equals("Visitante") ? p.getTimeVisitante() : "empate";
                processarRegistrarAposta(cbUsuariosAposta.getSelectedIndex(), cbPartidasAposta.getSelectedIndex(), timeVencedor, txtPlacarAposta.getText().trim(), false);
                txtPlacarAposta.setText("");
            } catch (Exception ex) {
                mostrarErro(ex.getMessage());
            }
        });
        return btnApostar;
    }
    private void exibirClassificacaoDoBanco(String nomeGrupo) {
        System.out.println("=========================================");
        System.out.println(" CLASSIFICAÇÃO: " + nomeGrupo.toUpperCase());
        System.out.println("=========================================");

        String sql = "SELECT u.nome, u.pontuacao FROM usuarios u " +
                "JOIN membros_grupo m ON u.id = m.usuario_id " +
                "WHERE m.grupo_nome = ? ORDER BY u.pontuacao DESC";

        try (Connection conn = ConexaoDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nomeGrupo);
            ResultSet rs = stmt.executeQuery();

            int posicao = 1;
            boolean temGente = false;

            while (rs.next()) {
                temGente = true;
                String nome = rs.getString("nome");
                int pontos = rs.getInt("pontuacao");
                System.out.println(posicao + "º Lugar: " + nome + " | Pontos: " + pontos);
                posicao++;
            }

            if (!temGente) {
                System.out.println("Nenhum usuário ativo neste grupo.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao gerar classificação: " + e.getMessage());
        }
        System.out.println("=========================================\n");
    }
}

