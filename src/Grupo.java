public class Grupo {

    private static final int MAX_GRUPOS   = 5;
    private static final int MAX_USUARIOS = 5;
    private static int totalGrupos        = 0;

    private String    nome;
    private Usuario[] usuarios;
    private int[]     pontuacaoNoGrupo;
    private int       quantidadeUsuarios;
    //garante que não tenha mais grupos que o limite
    public Grupo(String nome) {
        if (totalGrupos >= MAX_GRUPOS) {
            throw new IllegalStateException(
                    "Limite máximo de " + MAX_GRUPOS + " grupos atingido."
            );
        }
        this.nome               = nome;
        this.usuarios           = new Usuario[MAX_USUARIOS];
        this.pontuacaoNoGrupo   = new int[MAX_USUARIOS]; // Inicializando o array
        this.quantidadeUsuarios = 0;
        totalGrupos++;
    }

    //faz com que o usuario tenha seus pontos por grupo
    public void registrarPontosNoGrupo(Usuario usuario, int pontos) {
        for (int i = 0; i < quantidadeUsuarios; i++) {
            if (usuarios[i].equals(usuario)) {
                pontuacaoNoGrupo[i] += pontos;
                return;
            }
        }
    }
    //o grupo aceita o usuario caso não viole as regras
    boolean aceitarIngresso(Usuario usuario) {
        if (quantidadeUsuarios >= MAX_USUARIOS) {
            System.out.println("Grupo '" + nome + "' está cheio.");
            return false;
        }
        for (int i = 0; i < quantidadeUsuarios; i++) {
            if (usuarios[i].equals(usuario)) {
                System.out.println("Você já é membro do grupo '" + nome + "'.");
                return false;
            }
        }
        usuarios[quantidadeUsuarios] = usuario;
        pontuacaoNoGrupo[quantidadeUsuarios] = 0; // Zera a pontuação ao entrar
        quantidadeUsuarios++;
        return true;
    }
    //retira o usuario do grupo
    boolean processarSaida(Usuario usuario) {
        for (int i = 0; i < quantidadeUsuarios; i++) {
            if (usuarios[i].equals(usuario)) {
                // Desloca tanto o usuário quanto a pontuação dele
                for (int j = i; j < quantidadeUsuarios - 1; j++) {
                    usuarios[j] = usuarios[j + 1];
                    pontuacaoNoGrupo[j] = pontuacaoNoGrupo[j + 1];
                }
                usuarios[quantidadeUsuarios - 1] = null;
                pontuacaoNoGrupo[quantidadeUsuarios - 1] = 0;
                quantidadeUsuarios--;
                return true;
            }
        }
        System.out.println("Você não é membro do grupo '" + nome + "'.");
        return false;
    }
    //classifica os membros do grupo
    public void classificacao() {
        if (quantidadeUsuarios == 0) {
            System.out.println("Nenhum membro no grupo '" + nome + "'.");
            return;
        }

        Usuario[] ordenados = new Usuario[quantidadeUsuarios];
        int[]     pontos    = new int[quantidadeUsuarios];

        for (int i = 0; i < quantidadeUsuarios; i++) {
            ordenados[i] = usuarios[i];
            pontos[i]    = pontuacaoNoGrupo[i];
        }

        //A ordem é definida pelos pontos
        for (int i = 0; i < quantidadeUsuarios - 1; i++) {
            for (int j = 0; j < quantidadeUsuarios - 1 - i; j++) {
                if (pontos[j] < pontos[j + 1]) {
                    int tmpP         = pontos[j];
                    pontos[j]        = pontos[j + 1];
                    pontos[j + 1]    = tmpP;

                    Usuario tmpU     = ordenados[j];
                    ordenados[j]     = ordenados[j + 1];
                    ordenados[j + 1] = tmpU;
                }
            }
        }

        System.out.println("=== Classificação — " + nome + " ===");
        for (int i = 0; i < quantidadeUsuarios; i++) {
            System.out.printf("%dº  %-15s  ID: %s  |  %d pts no grupo  |  %d pts global%n",
                    i + 1,
                    ordenados[i].getNome(),
                    ordenados[i].getId(),
                    pontos[i],
                    ordenados[i].getPontuacao()
            );
        }
    }

    public boolean ehMembro(Usuario usuario) {
        for (int i = 0; i < quantidadeUsuarios; i++) {
            if (usuarios[i].equals(usuario)) return true;
        }
        return false;
    }

    public String getNome()                { return nome; }

    @Override
    public String toString() {
        return "Grupo: " + nome + " | Membros: " + quantidadeUsuarios + "/" + MAX_USUARIOS;
    }
}