package ch.fhnw.swc.mrs.data;

import ch.fhnw.swc.mrs.model.Movie;
import ch.fhnw.swc.mrs.model.PriceCategory;
import ch.fhnw.swc.mrs.model.RegularPriceCategory;
import ch.fhnw.swc.mrs.model.User;
import org.dbunit.*;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.*;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/** WS_DB-Testing.pdf - Tasks 2: Write your own integration test for MovieDao
 *  a) What do the individual test methods test?
 *  b) Do you have any questions? Raise them to the lecturer.
 */

public class ITMovieDao extends DBTestCase {

	/** Class under test: UserDAO. */
	private MovieDAO dao;
    private IDatabaseTester tester;     //IDatabaseTester, a DBunit object. Manages DB testing using .xml files.
    private Connection connection;      //package java.sql; --> connection to DB.

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM movies";
    //private static final String DB_CONNECTION = "jdbc:hsqldb:mem:mrs";            //local
    private static final String DB_CONNECTION = "jdbc:hsqldb:hsql://localhost/";    //hsqldb

	/** Create a new Integration Test object. */
	public ITMovieDao() {
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
		InputStream stream = this.getClass().getResourceAsStream("MovieDaoTestData.xml");
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
		dao = new SQLMovieDAO(connection);
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
        Movie movie = new Movie("Star Wars", LocalDate.now(), RegularPriceCategory.getInstance(), 0);
        movie.setId(2);      //removing second entry of DB.
        dao.delete(movie);
        
        r = s.executeQuery(COUNT_SQL);
        r.next();
        rows = r.getInt(1);
        assertEquals(2, rows);
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
        Movie movie = new Movie("Star Wars", LocalDate.now(), RegularPriceCategory.getInstance(), 0);
        movie.setId(2);     //delete 2nd entry.
        dao.delete(movie);

        
        // Fetch database data after deletion
        IDataSet databaseDataSet = tester.getConnection().createDataSet();
        ITable actualTable = databaseDataSet.getTable("MOVIES");

        InputStream stream = this.getClass().getResourceAsStream("MovieDaoTestResult.xml");  //gets expected DB status.
        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(stream);
        ITable expectedTable = expectedDataSet.getTable("MOVIES");

        Assertion.assertEquals(expectedTable, actualTable);
    }

    /*
        a) What does this test method so.
        1. gets a list of all users stored in the UserDAO
        2. gets expected result from DBunit .xml file.
        3. compares expected with current result set.
        In short: Does the DAO deliver the same entries as it is stored in the DB?
     */
//    public void testGetAll() throws DatabaseUnitException, SQLException, Exception {
//        List<Movie> movieList = dao.getAll();
//        ITable actualTable = convertToTable(movieList);      //ITable, a DBunit object. Probably storing comparable DB entries/values.
//
//        InputStream stream = this.getClass().getResourceAsStream("MovieDaoTestData.xml");
//        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(stream);
//        ITable expectedTable = expectedDataSet.getTable("MOVIES");
//
//        Assertion.assertEquals(expectedTable, actualTable);
//    }

    /*
        a) What does this test method do?
        Note: Single Row test means, there's only one entry in the DB (User table.to be precise).
        1. get expected result from DBunit .xml file.
        2. get all DB entries via UserDao. (Note: DAO execute SQL queries towards DB).
        3. checks that only one result is returned by the query.
        4. checks that the single entry is what is expected in UserDaoSingleRowTest.
     */
    public void testGetAllSingleRow() throws Exception {
        InputStream stream = this.getClass().getResourceAsStream("MovieDaoSingleRow.xml");
        IDataSet dataSet = new FlatXmlDataSetBuilder().build(stream);
        DatabaseOperation.CLEAN_INSERT.execute(tester.getConnection(), dataSet); //executes CRUD operations (?)

        List<Movie> movieList = dao.getAll();
        assertEquals(1, movieList.size());
        assertEquals("Casablanca", movieList.get(0).getTitle());
    }

    /*
        a) What does this test method do?
        The DB is empty. It is expected that the UserDAO returns an empty result list.
        This method checks whether this really is the case.
     */
    public void testGetAllEmptyTable() throws Exception {
    	InputStream stream = this.getClass().getResourceAsStream("MovieDaoEmpty.xml");
        IDataSet dataSet = new XmlDataSet(stream);
        DatabaseOperation.CLEAN_INSERT.execute(tester.getConnection(), dataSet);

        List<Movie> movieList = dao.getAll();
        assertNotNull(movieList);
        assertEquals(0, movieList.size());
    }

    /*
        a) What does this test method do?
        Checks wheter the UserDAO delivers the expected result by querying using the ID (or primary key).
     */
    public void testGetById() throws SQLException {
        Movie movie = dao.getById(2);
        assertEquals("Avatar", movie.getTitle());
        assertEquals("2005-09-17", movie.getReleaseDate().toString());
        assertEquals(2, (int) movie.getId());
    }

    /*
        a) What does this test method do?
        There are two entries with last name "Duck" in the DBunit's .xml file.
        This method checks whether the UserDAO delivers two entries.
        ("Does the SQL prepared statement work? Does the .getByName() work?)
     */
    public void testGetByName() throws SQLException {
        List<Movie> movieList = dao.getByTitle("Casablanca");
        assertEquals(1, movieList.size());
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

        Movie starwars = dao.getById(1);
        starwars.setTitle("Star Wars");
        dao.saveOrUpdate(starwars);
        Movie actual = dao.getById(1);
        assertEquals(starwars.getTitle(), actual.getTitle());

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

        Movie spacejam = new Movie("Space Jam", LocalDate.of(1936, 10, 12), RegularPriceCategory.getInstance(), 0);
        dao.saveOrUpdate(spacejam);
        Movie actual = dao.getById(spacejam.getId());
        assertEquals(spacejam.getTitle(), actual.getTitle());

        r = s.executeQuery(COUNT_SQL);
        r.next();
        int rows2 = r.getInt(1);
        assertEquals(rows1 + 1, rows2);
    }
    
	@SuppressWarnings("deprecation")
	private ITable convertToTable(List<Movie> movielist) throws Exception {
		ITableMetaData meta = new TableMetaData();
		DefaultTable t = new DefaultTable(meta);
		int row = 0;
		for (Movie m : movielist) {
			t.addRow();
			LocalDate d = m.getReleaseDate();
			/*			Expected :[agerating, id, isrented, pricecategory, releasedate, title]			 */
            t.setValue(row, "agerating", m.getAgeRating());
            t.setValue(row, "id", m.getId());               //wie in MovieDaoTestData.xml
            t.setValue(row, "isrented", m.isRented());
            t.setValue(row, "pricecategory", m.getPriceCategory().toString());
            t.setValue(row, "releasedate", new Date(d.getYear()-1900, d.getMonthValue()-1, d.getDayOfMonth()));
            t.setValue(row, "title", m.getTitle());
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
