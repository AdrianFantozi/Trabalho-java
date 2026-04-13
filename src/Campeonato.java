public class Campeonato {

    private static final int MAX_TIMES = 8;

    private String nome;
    private String[] times;
    private int quantidadeTimes;

    public Campeonato(String nome) {
        this.nome = nome;
        this.times = new String[MAX_TIMES];
        this.quantidadeTimes = 0;
    }
    //função que permite criar os times ja evitando que ultrapassem o limite ou que sejam duplicados
    public boolean adicionarTime(String nomeTime) {
        if (quantidadeTimes >= MAX_TIMES) {
            System.out.println("Limite máximo de " + MAX_TIMES + " times atingido.");
            return false;
        }
        for (int i = 0; i < quantidadeTimes; i++) {
            if (times[i].equalsIgnoreCase(nomeTime)) {
                System.out.println("Time '" + nomeTime + "' já está cadastrado.");
                return false;
            }
        }
        times[quantidadeTimes] = nomeTime;
        quantidadeTimes++;
        System.out.println("Time '" + nomeTime + "' adicionado com sucesso!");
        return true;
    }
    //removedor de times
    public boolean removerTime(String nomeTime) {
        for (int i = 0; i < quantidadeTimes; i++) {
            if (times[i].equalsIgnoreCase(nomeTime)) {
                for (int j = i; j < quantidadeTimes - 1; j++) {
                    times[j] = times[j + 1];
                }
                times[quantidadeTimes - 1] = null;
                quantidadeTimes--;
                System.out.println("Time '" + nomeTime + "' removido com sucesso!");
                return true;
            }
        }
        System.out.println("Time '" + nomeTime + "' não encontrado.");
        return false;
    }
    //função que garante que o time existe duranta a criação de partidas
    public boolean timeExiste(String nomeTime) {
        for (int i = 0; i < quantidadeTimes; i++) {
            if (times[i].equalsIgnoreCase(nomeTime)) {
                return true;
            }
        }
        return false;
    }

    public String getNome() {
        return nome;
    }
}

