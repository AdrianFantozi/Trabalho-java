public class Usuario {

    private static final int MAX_USUARIOS = 999;
    private static int contadorId         = 0;

    private String id;
    private String nome;
    private int    pontuacao;
    //criação do ususario
    public Usuario(String nome) {
        if (contadorId >= MAX_USUARIOS) {
            throw new IllegalStateException(
                    "Limite máximo de " + MAX_USUARIOS + " usuários atingido."
            );
        }
        contadorId++;
        this.id        = String.format("%03d", contadorId);
        this.nome      = nome;
        this.pontuacao = 0;
    }
    //utilizado para o usuario ingressar no grupo
    public boolean ingressar(Grupo grupo) {
        boolean aceito = grupo.aceitarIngresso(this);
        if (aceito) {
            System.out.println(nome + " (ID: " + id + ") ingressou no grupo '" + grupo.getNome() + "'.");
        }
        return aceito;
    }
    //utilizado para o usuario sair do grupo
    public boolean sair(Grupo grupo) {
        boolean saiu = grupo.processarSaida(this);
        if (saiu) {
            System.out.println(nome + " (ID: " + id + ") saiu do grupo '" + grupo.getNome() + "'.");
        }
        return saiu;
    }
    //a aposta adiciona os pontos ao usuario
    public void adicionarPontos(int pontos) {
        if (pontos <= 0) {
            System.out.println("A quantidade de pontos deve ser positiva.");
            return;
        }
        this.pontuacao += pontos;
        System.out.println("+" + pontos + " pts para " + nome + ". Total: " + pontuacao);
    }

    public static int totalUsuarios()   { return contadorId; }
    public static int vagasRestantes()  { return MAX_USUARIOS - contadorId; }

    public String getId()     { return id; }
    public String getNome()   { return nome; }
    public int getPontuacao() { return pontuacao; }
    public void setNome(String nome) { this.nome = nome; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Usuario)) return false;
        return this.id.equals(((Usuario) obj).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nome=" + nome + ", pts=" + pontuacao + "}";
    }
}