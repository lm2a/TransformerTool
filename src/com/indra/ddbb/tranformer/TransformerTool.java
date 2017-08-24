package com.indra.ddbb.tranformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class TransformerTool {

	static String mDatabase;
	static String mTable;
	static String mPath;
	static String mOriginName;
	static String mTargetName;
	static String mOriginPath;
	static String mTargetPath;
	Connection mOrigin;
	Connection mTarget;

	public static void main(String[] args) {
		TransformerTool tool = new TransformerTool();
		tool.readProperties();
		tool.common();
		tool.processTVoterStatus();
		tool.processTStatus();
		tool.processTServers();
		tool.processTParam();
		tool.processTLogEvent();
		tool.processTGeographic();
		tool.processTFingerprints();
		tool.processTEvent();
		tool.processTCenso();
	}

	private void readProperties() {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			mOriginName = prop.getProperty("database_origin");
			mTargetName = prop.getProperty("database_target");
			mPath = getPathInTheRightWay(prop.getProperty("path"));
			mOriginPath = mPath + mOriginName;
			mTargetPath = mPath + mTargetName;

			// get the property value and print it out
			System.out.println(mOriginName);
			System.out.println(mTargetName);
			System.out.println(mPath);
			System.out.println(mOriginPath);
			System.out.println(mTargetPath);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Check path ends with / or add it
	 * 
	 * @param path
	 * @return
	 */
	private static String getPathInTheRightWay(String path) {
		if (path == null || (path.length() < 3)) {
			path = "./";
		} else {
			if (path.charAt(path.length() - 1) != '/') {
				path = path + '/';
			}
		}
		return path;
	}

	/**
	 * Connect to the test.db database
	 * 
	 * @return the Connection object
	 */
	private Connection getDDBBConnectiont(String ddbb) {
		// SQLite connection string
		String url = String.format("jdbc:sqlite:%s", ddbb);
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}

	public void createNewDatabase(String fileName) {
		String url = String.format("jdbc:sqlite:%s", mPath + fileName);

		try (Connection conn = DriverManager.getConnection(url)) {
			if (conn != null) {
				DatabaseMetaData meta = conn.getMetaData();
				System.out.println("The driver name is " + meta.getDriverName());
				System.out.println("A new database has been created.");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void common() {
		mOrigin = getDDBBConnectiont(mOriginPath);
		deleteFile(mTargetPath);
		createNewDatabase("epb_base.db");
		mTarget = getDDBBConnectiont(mTargetPath);
	}

	public void processTVoterStatus() {
		createTVoterStatusTable();
		String sql = String.format("SELECT * FROM %s", "TVOTERSTATUS");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				int idvoterstatus = rs.getInt("idvoterstatus");
				String description = rs.getString("description");
				System.out.println("Leido: " + idvoterstatus + " / " + description);
				addRowTVoterStatus(idvoterstatus, description);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTVoterStatusTable() {
		String sql = "CREATE TABLE IF NOT EXISTS\n" + " T_VOTER_STATUS (\n"
				+ " IDVOTERSTATUS INTEGER PRIMARY KEY  NOT NULL,\n" + " DESCRIPTION VARCHAR NOT NULL\n" + ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * update TVoterStatus
	 * 
	 * @param idvoterstatus
	 * @param description
	 */
	public void addRowTVoterStatus(int idvoterstatus, String description) {
		System.out.println("Escrito: " + idvoterstatus + " / " + description);
		String sql = "INSERT INTO T_VOTER_STATUS (IDVOTERSTATUS, DESCRIPTION) VALUES(?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setInt(1, idvoterstatus);
			pstmt.setString(2, description);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void processTStatus() {
		createTStatusTable();
		String sql = String.format("SELECT * FROM %s", "TSTATUS");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				String polling_station = rs.getString("polling_station");
				int census = rs.getInt("census");
				int status = rs.getInt("status");
				String time_open = rs.getString("time_open");
				String time_close = rs.getString("time_close");
				String date_election = rs.getString("date_election");
				addRowTStatus(polling_station, census, status, time_open, time_close, date_election);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTStatusTable() {
		String sql = "CREATE TABLE IF NOT EXISTS\n" + " T_STATUS (\n" + " POLLING_STATION VARCHAR NOT NULL,\n"
				+ " CENSUS INTEGER NOT NULL,\n" + " STATUS INT (1) NOT NULL,\n" + " TIME_OPEN VARCHAR,\n"
				+ " TIME_CLOSE VARCHARL,\n" + " DATE_ELECTION VARCHARL\n" + ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * update TVoterStatus
	 * 
	 * @param idvoterstatus
	 * @param description
	 */
	public void addRowTStatus(String polling_station, int census, int status, String time_open, String time_close,
			String date_election) {
		System.out.println("Escrito: " + polling_station + " / " + census + " / " + status + " / " + time_open + " / "
				+ time_close + " / " + date_election);
		String sql = "INSERT INTO T_STATUS (POLLING_STATION, CENSUS, STATUS, TIME_OPEN, TIME_CLOSE, DATE_ELECTION) VALUES(?,  ?,  ?,  ?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {

			pstmt.setString(1, polling_station);
			pstmt.setInt(2, census);
			pstmt.setInt(3, status);
			pstmt.setString(4, time_open);
			pstmt.setString(5, time_close);
			pstmt.setString(6, date_election);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void processTServers() {
		createTServersTable();
		String sql = String.format("SELECT * FROM %s", "TSERVERS");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				String url_server = rs.getString("url_server");
				String username = rs.getString("username");
				String password = rs.getString("password");
				addRowTServers(url_server, username, password);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTServersTable() {
		String sql = "CREATE TABLE IF NOT EXISTS\n" + " T_SERVERS (" + " URL_SERVER VARCHAR NOT NULL,"
				+ " USERNAME VARCHAR," + " PASSWORD VARCHARL" + ");";
		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * add row to TServers
	 * 
	 * @param url_server
	 * @param username
	 * @param password
	 */
	public void addRowTServers(String url_server, String username, String password) {
		System.out.println("Escrito: " + url_server + " / " + username + " / " + password);
		String sql = "INSERT INTO T_SERVERS (URL_SERVER, USERNAME, PASSWORD) VALUES(?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setString(1, url_server);
			pstmt.setString(2, username);
			pstmt.setString(3, password);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void processTParam() {
		createTParamTable();
		String sql = String.format("SELECT * FROM %s", "TPARAM");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				int idparam = rs.getInt("idparam");
				String paramname = rs.getString("paramname");
				String paramtype = rs.getString("paramtype");
				String stringvalue = rs.getString("stringvalue");
				addRowTParam(idparam, paramname, paramtype, stringvalue);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTParamTable() {
		String sql = "CREATE TABLE IF NOT EXISTS" + " T_PARAM (" + " ID_PARAM INTEGER NOT NULL,"
				+ " PARAM_NAME VARCHAR NOT NULL," + " PARAM_TYPE VARCHAR," + " STRING_VALUE VARCHARL" + ");";
		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * add row to TParam
	 * 
	 * @param idparam
	 * @param paramname
	 * @param paramtype
	 * @param stringvalue
	 */
	public void addRowTParam(int idparam, String paramname, String paramtype, String stringvalue) {
		System.out.println("Escrito: " + idparam + " / " + paramname + " / " + paramtype + " / " + stringvalue);
		String sql = "INSERT INTO T_PARAM (ID_PARAM, PARAM_NAME, PARAM_TYPE, STRING_VALUE) VALUES(?,  ?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setInt(1, idparam);
			pstmt.setString(2, paramname);
			pstmt.setString(3, paramtype);
			pstmt.setString(4, stringvalue);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void processTLogEvent() {
		createTLogEventTable();
		String sql = String.format("SELECT * FROM %s", "TLOGEVENT");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				int id_log_event = rs.getInt("id_log_event");
				int id_user = rs.getInt("id_user");
				int user_role = rs.getInt("user_role");
				String sys_date = rs.getString("sys_date");
				String hashcode = rs.getString("hashcode");
				int idevent = rs.getInt("idevent");
				String log_message = rs.getString("log_message");
				addRowTLogEvent(id_log_event, id_user, user_role, sys_date, hashcode, idevent, log_message);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTLogEventTable() {
		String sql = "CREATE TABLE IF NOT EXISTS\n" + " T_LOG_EVENT (\n" + " ID_LOG_EVENT INTEGER NOT NULL,\n"
				+ " ID_USER INTEGER,\n" + " USER_ROLE INTEGER,\n" + " SYS_DATE VARCHAR,\n" + " HASHCODE VARCHARL,\n"
				+ " ID_EVENT INTEGER,\n" + " LOG_MESSAGE VARCHARL\n" + ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * update TLogEvent
	 * 
	 * @param id_log_event
	 * @param id_user
	 * @param user_role
	 * @param sys_date
	 * @param hashcode
	 * @param id_lidevent
	 * @param log_message
	 */
	public void addRowTLogEvent(int id_log_event, int id_user, int user_role, String sys_date, String hashcode,
			int idevent, String log_message) {
		System.out.println("Escrito: " + id_log_event + " / " + id_user + " / " + user_role + " / " + sys_date + " / "
				+ hashcode + " / " + idevent + " / " + log_message);
		String sql = "INSERT INTO T_LOG_EVENT (ID_LOG_EVENT, ID_USER, USER_ROLE, SYS_DATE, HASHCODE, ID_EVENT, LOG_MESSAGE) VALUES(?,  ?,  ?,  ?,  ?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {

			pstmt.setInt(1, id_log_event);
			pstmt.setInt(2, id_user);
			pstmt.setInt(3, user_role);
			pstmt.setString(4, sys_date);
			pstmt.setString(5, hashcode);
			pstmt.setInt(3, idevent);
			pstmt.setString(6, log_message);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	// --

	public void processTGeographic() {
		createTGeographicTable();
		String sql = String.format("SELECT * FROM %s", "TGEOGRAPHIC");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				String idprovince = rs.getString("idprovince");
				String province = rs.getString("province");
				String idmunicipality = rs.getString("idmunicipality");
				String municipality = rs.getString("municipality");
				String cod_polling_center = rs.getString("cod_polling_center");
				String polling_center = rs.getString("polling_center");
				String polling_station = rs.getString("polling_station");
				String cod_polling_station = rs.getString("cod_polling_station");
				addRowTGeographic(idprovince, province, idmunicipality, municipality, cod_polling_center,
						polling_center, polling_station, cod_polling_station);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTGeographicTable() {
		String sql = "CREATE TABLE IF NOT EXISTS" + " T_GEOGRAPHIC (" + "ID_PROVINCE VARCHAR,"+ " PROVINCE VARCHAR,"
				+ " ID_MUNICIPALITY VARCHAR," + " MUNICIPALITY VARCHAR," + " COD_POLLING_CENTER VARCHAR,"
				+ " POLLING_CENTER VARCHARL," + " POLLING_STATION VARCHAR," + " COD_POLLING_STATION VARCHARL"
				+ ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * update TGeographic
	 * 
	 * @param idprovince
	 * @param province
	 * @param idmunicipality
	 * @param municipality
	 * @param cod_polling_center
	 * @param polling_center
	 * @param polling_station
	 * @param cod_polling_station
	 */
	public void addRowTGeographic(String idprovince, String province, String idmunicipality, String municipality,
			String cod_polling_center, String polling_center, String polling_station, String cod_polling_station) {
		System.out.println("Escrito: " + idprovince + " / " + province + " / " + idmunicipality + " / " + municipality
				+ " / " + cod_polling_center + " / " + polling_center + " / " + polling_station + " / "
				+ cod_polling_station);
		String sql = "INSERT INTO T_GEOGRAPHIC (ID_PROVINCE, PROVINCE, ID_MUNICIPALITY, MUNICIPALITY, COD_POLLING_CENTER, POLLING_CENTER, POLLING_STATION, COD_POLLING_STATION) VALUES(?,  ?,  ?,  ?,  ?,  ?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setString(1, idprovince);
			pstmt.setString(2, province);
			pstmt.setString(3, idmunicipality);
			pstmt.setString(4, municipality);
			pstmt.setString(5, cod_polling_center);
			pstmt.setString(6, polling_center);
			pstmt.setString(7, polling_station);
			pstmt.setString(8, cod_polling_station);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void processTFingerprints() {
		createTFingerprintsTable();
		String sql = String.format("SELECT * FROM %s", "TFINGERPRINTS");

		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {

				String f64 = getBase64FromByteArray(rs.getBytes("fingerprintminutiae"));
				int finger_number = rs.getInt("finger");
				String sys_date = rs.getString("sysdate");
				int id_voter = rs.getInt("id_voter");
				addRowTFingerprints(f64, finger_number, sys_date, id_voter);

			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	private String getBase64FromByteArray(byte[] array) {
		if(array!=null){
			return Base64.encodeToString(array, Base64.DEFAULT);
		}
		return null;
	}

	public void createTFingerprintsTable() {
		String sql = "CREATE TABLE IF NOT EXISTS" + " T_FINGERPRINTS (" + "FP_NF_RECORD VARCHAR,"
				+ " FP_WSQ VARCHAR," + " FINGER_NUMBER INTEGER," + " SYS_DATE VARCHAR," + " ID_VOTER INTEGER,"
				+ " FP_ISO_CARD VARCHAR" + ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void addRowTFingerprints(String f64, int finger_number, String sys_date, int id_voter) {
		String sql = "INSERT INTO T_FINGERPRINTS (FP_NF_RECORD, FP_WSQ, FINGER_NUMBER, SYS_DATE, ID_VOTER, FP_ISO_CARD) VALUES(?,  ?,  ?,  ?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setString(1, f64);
			pstmt.setString(2, null);
			pstmt.setInt(3, finger_number);
			pstmt.setString(4, sys_date);
			pstmt.setInt(5, id_voter);
			pstmt.setString(6, null);
			


			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void processTEvent() {
		createTEventTable();
		String sql = String.format("SELECT * FROM %s", "TEVENT");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				int idevent = rs.getInt("idevent");
				String description = rs.getString("description");
				System.out.println("Leido: " + idevent + " / " + description);
				addRowTVoterStatus(idevent, description);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTEventTable() {
		String sql = "CREATE TABLE IF NOT EXISTS" + " T_EVENT (" + " ID_EVENT INTEGER PRIMARY KEY  NOT NULL,"
				+ " DESCRIPTION VARCHAR NOT NULL" + ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Add row to TEvent
	 * 
	 * @param idevent
	 * @param description
	 */
	public void addRowTEvent(int idevent, String description) {
		System.out.println("Escrito: " + idevent + " / " + description);
		String sql = "INSERT INTO T_EVENT (ID_EVENT, DESCRIPTION) VALUES(?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setInt(1, idevent);
			pstmt.setString(2, description);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	// ---

	public void processTCenso() {
		createTCensoTable();
		String sql = String.format("SELECT * FROM %s", "TCENSO");
		try (Statement stmt = mOrigin.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				int id_voter = rs.getInt("id_voter");
				String firstname = rs.getString("firstname");
				String middlename = rs.getString("middlename");
				String lastname = rs.getString("lastname");
				int has_voted = rs.getInt("has_voted");
				String sysdate = rs.getString("sysdate");
				String date_time_voted = rs.getString("date_time_voted");
				int gender = rs.getInt("gender");
				String birthdate = rs.getString("birthdate");
				int vote_type = rs.getInt("vote_type");
				int idvoterstatus = rs.getInt("idvoterstatus");

				String document_scan = getBase64FromByteArray(rs.getBytes("document_scan"));

				String votercard = rs.getString("votercard");
				int censuspage = rs.getInt("censuspage");
				int censusposition = rs.getInt("censusposition");
				int newvoter = rs.getInt("Newvoter");
				addRowTCenso(id_voter, firstname, middlename, lastname, has_voted, sysdate, date_time_voted, gender,
						birthdate, vote_type, idvoterstatus, document_scan, votercard, censuspage, censusposition,
						newvoter);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	public void createTCensoTable() {
		String sql = "CREATE TABLE IF NOT EXISTS" + " T_CENSO (" + " ID_VOTER INTEGER,"
				+ " FIRST_NAME VARCHAR," + " MIDDLE_NAME VARCHAR," + " LAST_NAME VARCHAR,"
				+ " HAS_VOTED INTEGER," + " SYS_DATE VARCHAR," + " DATE_TIME_VOTED VARCHAR,"
				+ " GENDER INTEGER," + " BIRTH_DATE VARCHAR," + " VOTE_TYPE INTEGER,"
				+ " ID_VOTER_STATUS INTEGER," + " DOCUMENT_SCAN VARCHAR," + " VOTER_CARD VARCHAR,"
				+ " NEW_VOTER INTEGER" + ");";

		try (Statement stmt = mTarget.createStatement()) {
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * update TVoterStatus
	 * 
	 * @param idvoterstatus
	 * @param description
	 */
	public void addRowTCenso(int id_voter, String firstname, String middlename, String lastname, int has_voted,
			String sysdate, String date_time_voted, int gender, String birthdate, int vote_type, int idvoterstatus,
			String document_scan, String votercard, int censuspage, int censusposition, int newvoter) {
		System.out.println("Escrito: " + id_voter + " / " + firstname + " / " + middlename + " / " + lastname + " / "
				+ has_voted + " / " + sysdate + " / " + date_time_voted + " / " + gender + " / " + birthdate + " / "
				+ vote_type + idvoterstatus + " / " + " / " + votercard + " / " + censuspage + " / "
				+ censusposition + " / " + newvoter);
		String sql = "INSERT INTO T_CENSO (ID_VOTER, FIRST_NAME, MIDDLE_NAME, LAST_NAME, HAS_VOTED, SYS_DATE, DATE_TIME_VOTED, GENDER, BIRTH_DATE, VOTE_TYPE, ID_VOTER_STATUS, DOCUMENT_SCAN, VOTER_CARD, NEW_VOTER) VALUES(?, ?,  ?,  ?,  ?,  ?,  ?, ?,  ?,  ?,  ?,  ?,  ?,  ?)";
		try (PreparedStatement pstmt = mTarget.prepareStatement(sql)) {
			pstmt.setInt(1, id_voter);
			pstmt.setString(2, firstname);
			pstmt.setString(3, middlename);
			pstmt.setString(4, lastname);
			pstmt.setInt(5, has_voted);
			pstmt.setString(6, sysdate);
			pstmt.setString(7, date_time_voted);
			pstmt.setInt(8, gender);
			pstmt.setString(9, birthdate);
			pstmt.setInt(10, vote_type);
			pstmt.setInt(11, idvoterstatus);
			pstmt.setString(12, document_scan);
			pstmt.setString(13, votercard);
//			pstmt.setInt(14, censuspage);
//			pstmt.setInt(15, censusposition);
			pstmt.setInt(14, newvoter);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private void deleteFile(String filePath){
	try{

		File file = new File(filePath);

		if(file.delete()){
			System.out.println(file.getName() + " is deleted!");
		}else{
			System.out.println("Delete operation is failed.");
		}

	}catch(Exception e){

		e.printStackTrace();

	}
	}
}
