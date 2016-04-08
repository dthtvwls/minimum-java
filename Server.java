import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import javax.json.*;
import com.sun.net.httpserver.HttpServer;

public class Server {
    public static void main(String[] args) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:" + System.getenv("DATABASE_URL"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/").setHandler(httpExchange -> {
            try {
                PreparedStatement stmt = connection.prepareStatement("select ? success");
                stmt.setBoolean(1, true);

                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                ArrayList<String> columnNames = new ArrayList<String>();
                ArrayList<String> columnClassNames = new ArrayList<String>();

                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(rsmd.getColumnName(i));
                    columnClassNames.add(rsmd.getColumnClassName(i));
                }

                JsonArrayBuilder jab = Json.createArrayBuilder();

                while (rs.next()) {
                    JsonObjectBuilder job = Json.createObjectBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = columnNames.get(i - 1);
                        switch (columnClassNames.get(i - 1)) {
                            case "java.sql.Timestamp":
                                job.add(columnName, rs.getTimestamp(i).getTime());
                                break;
                            case "java.sql.Date":
                                job.add(columnName, rs.getDate(i).toString());
                                break;
                            case "java.math.BigDecimal":
                                job.add(columnName, rs.getBigDecimal(i));
                                break;
                            case "java.lang.Boolean":
                                job.add(columnName, rs.getBoolean(i));
                                break;
                            case "java.lang.Integer":
                                job.add(columnName, rs.getInt(i));
                                break;
                            case "java.lang.Float":
                                job.add(columnName, rs.getFloat(i));
                                break;
                            case "java.lang.Long":
                                job.add(columnName, rs.getLong(i));
                                break;
                            case "java.lang.String":
                                job.add(columnName, rs.getString(i));
                                break;
                        }
                    }
                    jab.add(job);
                }

                byte[] bytes = jab.build().toString().getBytes();
            	httpExchange.sendResponseHeaders(200, bytes.length);
            	httpExchange.getResponseBody().write(bytes);
                httpExchange.close();
                rs.close();
                stmt.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        });

        server.start();
    }
}
