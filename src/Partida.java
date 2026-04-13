import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Partida {

    private static final int MAX_APOSTAS  = 100;
    private static int contadorPartida    = 0;

    private String    id;
    private String    nomePartida;
    private String    timeMandante;
    private String    timeVisitante;
    private LocalDate data;
    private LocalTime horario;
    private int       golsMandante;
    private int       golsVisitante;
    private boolean   realizada;
    private Aposta[]  apostas;
    private int       totalApostas;
    //construtor
    public Partida(Campeonato campeonato, String timeMandante, String timeVisitante,
                   LocalDate data, LocalTime horario) {

        if (!campeonato.timeExiste(timeMandante)) {
            throw new IllegalArgumentException("Time '" + timeMandante + "' não está cadastrado.");
        }
        if (!campeonato.timeExiste(timeVisitante)) {
            throw new IllegalArgumentException("Time '" + timeVisitante + "' não está cadastrado.");
        }
        if (timeMandante.equalsIgnoreCase(timeVisitante)) {
            throw new IllegalArgumentException("Um time não pode jogar contra si mesmo.");
        }

        contadorPartida++;
        this.id            = String.format("PAR-%03d", contadorPartida);
        this.timeMandante  = timeMandante;
        this.timeVisitante = timeVisitante;
        this.nomePartida   = timeMandante + " X " + timeVisitante;
        this.data          = data;
        this.horario       = horario;
        this.golsMandante  = 0;
        this.golsVisitante = 0;
        this.realizada     = false;
        this.apostas       = new Aposta[MAX_APOSTAS];
        this.totalApostas  = 0;
    }

    //Vinculado pela aposta no momento em que ela é criada
    public void vincularAposta(Aposta aposta) {
        if (totalApostas >= MAX_APOSTAS) {
            System.out.println("Limite de apostas para esta partida atingido.");
            return;
        }
        apostas[totalApostas] = aposta;
        totalApostas++;
    }
    //responsavel por registrar a partida e encerrar
    public void registrarResultado(int golsMandante, int golsVisitante) {
        if (realizada) {
            System.out.println("Esta partida já foi realizada.");
            return;
        }
        if (golsMandante < 0 || golsVisitante < 0) {
            System.out.println("Número de gols não pode ser negativo.");
            return;
        }
        this.golsMandante  = golsMandante;
        this.golsVisitante = golsVisitante;
        this.realizada     = true;
        System.out.println("Resultado registrado: " + getResultado());

        // Partida notifica cada aposta vinculada
        System.out.println("\nNotificando " + totalApostas + " aposta(s)...");
        for (int i = 0; i < totalApostas; i++) {
            apostas[i].notificar(this);
        }
    }
    //verificação de resultados das partidas ou se ainda não ocorreu
    public String getResultado() {
        if (!realizada) return nomePartida + " — Partida não realizada";
        String vencedor;
        if (golsMandante > golsVisitante)       vencedor = "Vencedor: " + timeMandante;
        else if (golsVisitante > golsMandante)  vencedor = "Vencedor: " + timeVisitante;
        else                                    vencedor = "Empate";
        return nomePartida + " | " + golsMandante + " x " + golsVisitante + " | " + vencedor;
    }

    public String getDataHora() {
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " às "
                + horario.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public String toString() {
        return "=== " + nomePartida + " ===" +
                "\nID        : " + id +
                "\nData/Hora : " + getDataHora() +
                "\nStatus    : " + (realizada ? getResultado() : "Aguardando realização");
    }

    public String    getId()            { return id; }
    public String    getNomePartida()   { return nomePartida; }
    public String    getTimeMandante()  { return timeMandante; }
    public String    getTimeVisitante() { return timeVisitante; }
    public LocalDate getData()          { return data; }
    public LocalTime getHorario()       { return horario; }
    public int       getGolsMandante()  { return golsMandante; }
    public int       getGolsVisitante() { return golsVisitante; }
    public boolean   isRealizada()      { return realizada; }
}