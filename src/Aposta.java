import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public class Aposta {

    private static int contadorAposta = 0;
    //define como esta a aposta
    public enum Status {
        PENDENTE,
        ERROU,
        ACERTOU_TIME,
        ACERTOU_TUDO
    }
    //define a regra de pontuação
    private static final int PONTOS_ACERTOU_TIME = 5;
    private static final int PONTOS_ACERTOU_TUDO = 10;
    //atributos da classe
    private String        id;
    private Usuario       usuario;
    private Partida       partida;
    private Grupo[]       grupos;
    private int           totalGrupos;
    private String        timeGanhador;
    private String        resultado;
    private LocalDateTime momentoAposta;
    private Status        status;

    //Construtor

    public Aposta(Usuario usuario, Partida partida, Grupo[] grupos, int totalGrupos,
                  String timeGanhador, String resultado) {

        validarPartidaRegistrada(partida);
        validarPrazo(partida);
        validarTimeGanhador(partida, timeGanhador);
        validarFormatoResultado(resultado);
        //garante que cada aposta tenha um id unico para não existir burlas
        contadorAposta++;
        this.id           = String.format("APO-%04d", contadorAposta);
        this.usuario      = usuario;
        this.partida      = partida;
        this.grupos       = grupos;
        this.totalGrupos  = totalGrupos;
        this.timeGanhador = timeGanhador;
        this.resultado    = resultado;
        this.momentoAposta = LocalDateTime.now();
        this.status       = Status.PENDENTE;

        //garante a atualização da aposta ao encerramento da partida
        partida.vincularAposta(this);

        System.out.println("Aposta " + id + " registrada — aguardando resultado da partida.");
    }

    //a função que faz a notificação e que confere os resultados das apostas

    public void notificar(Partida partida) {
        if (status != Status.PENDENTE) {
            System.out.println("Aposta " + id + " já foi resolvida: " + status);
            return;
        }

        String  ganhadorReal  = resolverGanhadorReal(partida);
        String  resultadoReal = partida.getGolsMandante() + "x" + partida.getGolsVisitante();
        boolean acertouTime   = timeGanhador.equalsIgnoreCase(ganhadorReal);
        boolean acertouPlacar = resultado.equalsIgnoreCase(resultadoReal);
        int     pontosGanhos  = 0;

        if (acertouTime && acertouPlacar) {
            status       = Status.ACERTOU_TUDO;
            pontosGanhos = PONTOS_ACERTOU_TUDO;
            System.out.println("[" + id + "] " + usuario.getNome()
                    + " acertou time E placar! +" + pontosGanhos + " pts");

        } else if (acertouTime) {
            status       = Status.ACERTOU_TIME;
            pontosGanhos = PONTOS_ACERTOU_TIME;
            System.out.println("[" + id + "] " + usuario.getNome()
                    + " acertou o time ganhador! +" + pontosGanhos + " pts");

        } else {
            status = Status.ERROU;
            System.out.println("[" + id + "] " + usuario.getNome()
                    + " errou. Ganhador real: " + ganhadorReal
                    + " | Placar real: " + resultadoReal);
        }

        if (pontosGanhos > 0) {
            usuario.adicionarPontos(pontosGanhos);
            for (int i = 0; i < totalGrupos; i++) {
                if (grupos[i].ehMembro(usuario)) {
                    grupos[i].registrarPontosNoGrupo(usuario, pontosGanhos);
                    System.out.println("  -> Pontos registrados no grupo '" + grupos[i].getNome() + "'.");
                }
            }
        }
    }

    private String resolverGanhadorReal(Partida partida) {
        if (partida.getGolsMandante() > partida.getGolsVisitante()) return partida.getTimeMandante();
        if (partida.getGolsVisitante() > partida.getGolsMandante()) return partida.getTimeVisitante();
        return "empate";
    }

    // tratamento de erros que os usuarios podem cometer

    private void validarPartidaRegistrada(Partida partida) {
        if (partida == null) {
            throw new IllegalArgumentException("A partida informada não existe no sistema.");
        }
        if (partida.isRealizada()) {
            throw new IllegalStateException(
                    "A partida '" + partida.getNomePartida() + "' já foi realizada. Apostas encerradas."
            );
        }
    }

    private void validarPrazo(Partida partida) {
        LocalDateTime inicioPartida    = LocalDateTime.of(partida.getData(), partida.getHorario());
        LocalDateTime limiteAposta     = inicioPartida.minusMinutes(20);
        LocalDateTime agora            = LocalDateTime.now();
        long          minutosRestantes = ChronoUnit.MINUTES.between(agora, inicioPartida);

        if (agora.isAfter(limiteAposta)) {
            throw new IllegalStateException(
                    "Prazo encerrado! Apostas se encerram 20 minutos antes da partida. "
                            + "Faltam " + minutosRestantes + " minuto(s) para o início."
            );
        }
        System.out.println("Prazo OK — faltam " + minutosRestantes + " minuto(s) para a partida.");
    }

    private void validarTimeGanhador(Partida partida, String timeGanhador) {
        boolean ehMandante  = partida.getTimeMandante().equalsIgnoreCase(timeGanhador);
        boolean ehVisitante = partida.getTimeVisitante().equalsIgnoreCase(timeGanhador);
        boolean ehEmpate    = timeGanhador.equalsIgnoreCase("empate");

        if (!ehMandante && !ehVisitante && !ehEmpate) {
            throw new IllegalArgumentException(
                    "Time ganhador inválido. Opções: '"
                            + partida.getTimeMandante() + "', '"
                            + partida.getTimeVisitante() + "' ou 'empate'."
            );
        }
    }

    private void validarFormatoResultado(String resultado) {
        if (resultado == null || !resultado.matches("\\d+x\\d+")) {
            throw new IllegalArgumentException(
                    "Formato inválido: '" + resultado + "'. Use '2x1', '0x0', etc."
            );
        }
    }

    //Getters

    public String        getId()            { return id; }
    public Usuario       getUsuario()       { return usuario; }
    public Partida       getPartida()       { return partida; }
    public String        getTimeGanhador()  { return timeGanhador; }
    public String        getResultado()     { return resultado; }
    public Status        getStatus()        { return status; }
    public LocalDateTime getMomentoAposta() { return momentoAposta; }
    public static int    totalApostas()     { return contadorAposta; }

    @Override
    public String toString() {
        return "=== Aposta " + id + " ===" +
                "\nUsuário       : " + usuario.getNome() + " (ID: " + usuario.getId() + ")" +
                "\nPartida       : " + partida.getNomePartida() +
                "\nData/Hora     : " + partida.getDataHora() +
                "\nPalpite time  : " + timeGanhador +
                "\nPalpite placar: " + resultado +
                "\nStatus        : " + status +
                "\nFeita em      : " + momentoAposta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}