package ch.fhnw.swc.mrs.data;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.dbunit.Assertion;
import org.dbunit.DBTestCase;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.NoSuchColumnException;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.xml.sax.InputSource;

import ch.fhnw.swc.mrs.model.PriceCategory;
import ch.fhnw.swc.mrs.model.User;


/** WS_DB-Testing.pdf - Tasks 2: Write your own integration test for MovieDao
 *  a) What do the individual test methods test?
 *  b) Do you have any questions? Raise them to the lecturer.
 */

public class ITUserDao extends DBTestCase {

	/** Class under test: UserDAO. */
	private UserDAO dao;
    private IDatabaseTester tester;     //IDatabaseTester, a DBunit object. Manages DB testing using .xml files.
    private Connection connection;      //package java.sql; --> connection to DB.

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM clients";
    //private static final String DB_CONNECTION = "jdbc:hsqldb:mem:mrs";            //local
    private static final String DB_CONNECTION = "jdbc:hsqldb:hsql://localhost/";    //hsqldb

	/** Create a new Integration Test object. */
	public ITUserDao() {
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, HsqlDatabase.DB_DRIVER);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, DB_CONNECTION);
		System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, "sa");
		System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, "");
	}    
    
	@Override
	protected void setUpDatabaseConfig(DatabaseConfig config) {
		config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
	}

	@Override
	protected IDataSet getDataSet() throws Exception {
		InputStream stream = this.getClass().getResourceAsStream("UserDaoTestData.xml");
		return new FlatXmlDataSetBuilder().build(new InputSource(stream));
	}

	static {
		try {
			new HsqlDatabase().initDB(DB_CONNECTION);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize a DBUnit DatabaseTester object to use in tests.
	 * 
	 * @throws Exception
	 *             whenever something goes wrong.
	 */
	public void setUp() throws Exception {
		super.setUp();
        PriceCategory.init();
        tester = new JdbcDatabaseTester(HsqlDatabase.DB_DRIVER, DB_CONNECTION);
		connection = getConnection().getConnection();
		dao = new SQLUserDAO(connection);
	}

	public void tearDown() throws Exception {
		connection.close();
		tester.onTearDown();
	}

	/*
	    a) What does this test method do?
	    1. count no. of rows before a DB entry gets deleted. Are there really 3 entries?
	    2. Tries to delete non-existing entry. Does any error occur?
	    3. ??? -> b) raised and solved. See comment below on line 121.
	 */
    public void testDeleteNonexisting() throws Exception {
        // count no. of rows before deletion
        Statement s = connection.createStatement();
        ResultSet r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows = r.getInt(1);
        assertEquals(3, rows);

        // Delete non-existing record
        User user = new User("Denzler", "Christoph", LocalDate.now());
        user.setId(42);
        dao.delete(user);
        
        r = s.executeQuery(COUNT_SQL);
        r.next();
        rows = r.getInt(1);
        assertEquals(2, rows);          // b) Why 2 and not 3? Shouldn't DB be free of changes?
                                                /*
                                                    Antwort (siehe UserDaoTestData.xml):
                                                    <clients id="42"
                                                    firstname="Micky"
                                                    name="Mouse"
                                                    birthdate="1935-11-03"/>

                                                    Mit user.setId(42) wird der name "Micky" vs. "Christian"
                                                    umgangen.
                                                 */
    }
    
    /*
        a) What does this test method do?
        1. Check how many entries are at the beginning. 3 entries expected.
        2. Entry deleted according to ID. Two more entries left.
        3. Gets DBunit xml with expected DB status.
        4. compares current DB status to expected one.
     */
    public void testDelete() throws Exception {
        // count no. of rows before deletion
        Statement s = connection.createStatement();
        ResultSet r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows = r.getInt(1);
        assertEquals(3, rows);

        // delete existing record
        User user = new User("Duck", "Donald", LocalDate.of(2013, 1, 13));
        user.setId(13);
        dao.delete(user);

        /*
            <clients id="13"
            firstname="Donald"
            name="Duck"
            birthdate="2013-01-13"/>

            ...wird gelöscht
         */
        
        // Fetch database data after deletion
        IDataSet databaseDataSet = tester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("CLIENTS");

        InputStream stream = this.getClass().getResourceAsStream("UserDaoTestResult.xml");  //gets expected DB status.
        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(stream);
        ITable expectedTable = expectedDataSet.getTable("CLIENTS");

        Assertion.assertEquals(expectedTable, actualTable);
    }

    /*
        a) What does this test method so.
        1. gets a list of all users stored in the UserDAO
        2. gets expected result from DBunit .xml file.
        3. compares expected with current result set.
        In short: Does the DAO deliver the same entries as it is stored in the DB?
     */
    public void testGetAll() throws DatabaseUnitException, SQLException, Exception {
        List<User> userlist = dao.getAll();
        ITable actualTable = convertToTable(userlist);      //ITable, a DBunit object. Probably storing comparable DB entries/values.

        InputStream stream = this.getClass().getResourceAsStream("UserDaoTestData.xml");
        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(stream);
        ITable expectedTable = expectedDataSet.getTable("CLIENTS");

        Assertion.assertEquals(expectedTable, actualTable);
    }

    /*
        a) What does this test method do?
        Note: Single Row test means, there's only one entry in the DB (User table.to be precise).
        1. get expected result from DBunit .xml file.
        2. get all DB entries via UserDao. (Note: DAO execute SQL queries towards DB).
        3. checks that only one result is returned by the query.
        4. checks that the single entry is what is expected in UserDaoSingleRowTest.
     */
    public void testGetAllSingleRow() throws Exception {
        InputStream stream = this.getClass().getResourceAsStream("UserDaoSingleRowTest.xml");
        IDataSet dataSet = new FlatXmlDataSetBuilder().build(stream);
        DatabaseOperation.CLEAN_INSERT.execute(tester.getConnection(), dataSet); //executes CRUD operations (?)

        List<User> userlist = dao.getAll();
        assertEquals(1, userlist.size());
        assertEquals("Bond", userlist.get(0).getName());
    }

    /*
        a) What does this test method do?
        The DB is empty. It is expected that the UserDAO returns an empty result list.
        This method checks whether this really is the case.
     */
    public void testGetAllEmptyTable() throws Exception {
    	InputStream stream = this.getClass().getResourceAsStream("UserDaoEmpty.xml");
        IDataSet dataSet = new XmlDataSet(stream);
        DatabaseOperation.CLEAN_INSERT.execute(tester.getConnection(), dataSet);

        List<User> userlist = dao.getAll();
        assertNotNull(userlist);
        assertEquals(0, userlist.size());
    }

    /*
        a) What does this test method do?
        Checks wheter the UserDAO delivers the expected result by querying using the ID (or primary key).
     */
    public void testGetById() throws SQLException {
        User user = dao.getById(42);
        assertEquals("Micky", user.getFirstName());
        assertEquals("Mouse", user.getName());
        assertEquals(42, user.getId());
    }

    /*
        a) What does this test method do?
        There are two entries with last name "Duck" in the DBunit's .xml file.
        This method checks whether the UserDAO delivers two entries.
        ("Does the SQL prepared statement work? Does the .getByName() work?)
     */
    public void testGetByName() throws SQLException {
        List<User> userlist = dao.getByName("Duck");
        assertEquals(2, userlist.size());
    }

    /*
        a) What does this test method do?
        Checks whether dao.saveOrUpdate() follows the Update - behavior.
     */
    public void testUpdate() throws SQLException {
        // count no. of rows before operation
        Statement s = connection.createStatement(); //.createStatement() is a factory method.
        ResultSet r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows0 = r.getInt(1);

        User daisy = dao.getById(13);
        daisy.setFirstName("Daisy");
        dao.saveOrUpdate(daisy);
        User actual = dao.getById(13);
        assertEquals(daisy.getFirstName(), actual.getFirstName());

        r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows1 = r.getInt(1);
        assertEquals(rows0, rows1);
    }

    /*
        a) What does this test method do?
        Checks whether dao.saveOrUpdate() follows the Save/Create - behavior.
     */
    public void testSave() throws Exception {
        // count no. of rows before operation
        Statement s = connection.createStatement();
        ResultSet r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows1 = r.getInt(1);

        User goofy = new User("Goofy", "Goofus", LocalDate.of(1936, 10, 12));
        dao.saveOrUpdate(goofy);
        User actual = dao.getById(goofy.getId());
        assertEquals(goofy.getFirstName(), actual.getFirstName());

        r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows2 = r.getInt(1);
        assertEquals(rows1 + 1, rows2);
    }
    
	@SuppressWarnings("deprecation")
	private ITable convertToTable(List<User> userlist) throws Exception {
		ITableMetaData meta = new TableMetaData();
		DefaultTable t = new DefaultTable(meta);
		int row = 0;
		for (User u : userlist) {
			t.addRow();
			LocalDate d = u.getBirthdate();
			t.setValue(row, "id", u.getId());
			t.setValue(row, "name", u.getName());
			t.setValue(row, "firstname", u.getFirstName());
			t.setValue(row, "birthdate", new Date(d.getYear()-1900, d.getMonthValue()-1, d.getDayOfMonth()));
			row++;
		}
		return t;
	}

	private static final class TableMetaData implements ITableMetaData {

		private List<Column> cols = new ArrayList<>();

		TableMetaData() {
			cols.add(new Column("id", DataType.INTEGER));
			cols.add(new Column("name", DataType.VARCHAR));
			cols.add(new Column("firstname", DataType.VARCHAR));
			cols.add(new Column("birthdate", DataType.DATE));
		}

		@Override
		public int getColumnIndex(String colname) throws DataSetException {
			int index = 0;
			for (Column c : cols) {
				if (c.getColumnName().equals(colname.toLowerCase())) {
					return index;
				}
				index++;
			}
			throw new NoSuchColumnException(getTableName(), colname);
		}

		@Override
		public Column[] getColumns() throws DataSetException {
			return cols.toArray(new Column[4]);
		}

		@Override
		public Column[] getPrimaryKeys() throws DataSetException {
			Column[] cols = new Column[1];
			cols[0] = new Column("id", DataType.INTEGER);
			return cols;
		}

		@Override
		public String getTableName() {
			return "clients";
		}
	}
}
