import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoDB {


    private static final String URL = "jdbc:h2:./dados_bolao_db;AUTO_SERVER=TRUE";
    private static final String USER = "1";
    private static final String PASSWORD = "1";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao conectar com o banco de dados: " + e.getMessage());
        }
    }
}