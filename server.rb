$CLASSPATH << 'lib/mysql-connector-java-5.1.38-bin.jar'
$CLASSPATH << 'lib/javax.json-1.0.4.jar'

connection = java.sql.DriverManager.getConnection "jdbc:#{ENV['DATABASE_URL']}"
server = com.sun.net.httpserver.HttpServer.create java.net.InetSocketAddress.new(8080), 0

server.createContext('/').setHandler lambda { |httpExchange|
  stmt = connection.prepareStatement 'select ? success'
  stmt.setBoolean 1, true

  rs = stmt.executeQuery
  rsmd = rs.getMetaData
  columnCount = rsmd.getColumnCount
  columnNames = java.util.ArrayList.new
  columnClassNames = java.util.ArrayList.new

  (1..columnCount).each do |i|
    columnNames.add rsmd.getColumnName i
    columnClassNames.add rsmd.getColumnClassName i
  end

  jab = javax.json.Json.createArrayBuilder

  while rs.next do
    job = javax.json.Json.createObjectBuilder

    (1..columnCount).each do |i|
      columnName = columnNames.get i - 1

      case columnClassNames.get i - 1
      when 'java.sql.Timestamp'
        job.add columnName, rs.getTimestamp(i).getTime
      when 'java.sql.Date'
        job.add columnName, rs.getDate(i).toString
      when 'java.math.BigDecimal'
        job.add columnName, rs.getBigDecimal(i)
      when 'java.lang.Boolean'
        job.add columnName, rs.getBoolean(i)
      when 'java.lang.Integer'
        job.add columnName, rs.getInt(i)
      when 'java.lang.Float'
        job.add columnName, rs.getFloat(i)
      when 'java.lang.Long'
        job.add columnName, rs.getLong(i)
      when 'java.lang.String'
        job.add columnName, rs.getString(i)
      end
    end

    jab.add job
  end

  bytes = java.lang.String.new(jab.build.toString).getBytes
  httpExchange.sendResponseHeaders 200, bytes.length
  httpExchange.getResponseBody.write bytes
  httpExchange.close
  rs.close
  stmt.close
}

server.start
