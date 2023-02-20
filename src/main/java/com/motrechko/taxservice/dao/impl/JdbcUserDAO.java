package com.motrechko.taxservice.dao.impl;

import com.motrechko.taxservice.dao.ConnectionFactory;
import com.motrechko.taxservice.dao.UserDAO;
import com.motrechko.taxservice.model.User;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * An implementation of the UserDAO interface that uses JDBC to interact with a database.
 */
public class JdbcUserDAO implements UserDAO {
    private static final Logger logger = LogManager.getLogger(JdbcUserDAO.class);

    /**
     * Creates a new user in the database.
     *
     * @param user the user to create
     * @return true if the user was created successfully, false otherwise
     * @throws MySQLException if an error occurs while interacting with the database
     */
    @Override
    public boolean create(User user) throws MySQLException {
        Connection connection = null;
        try {
            connection = ConnectionFactory.getConnection(false);
            addUser(connection,user);
            connection.commit();
            logger.info("User was created successfully with email: " + user.getEmail());
            return true;
        } catch (SQLException | MySQLException e){
            ConnectionFactory.rollback(connection);
            logger.warn("Failed to insert new user with email: " + user.getEmail(), e);
            throw new MySQLException("Cannot add new User",e);
        }
        finally {
            ConnectionFactory.close(connection);
        }
    }

    /**
     * Adds a user to the database.
     *
     * @param connection the connection to use to interact with the database
     * @param user       the user to add
     * @throws MySQLException if an error occurs while interacting with the database
     */
    private void addUser(Connection connection, User user) throws MySQLException {
        try (PreparedStatement statement = connection.prepareStatement(MySQLQuery.INSERT_INTO_USER,Statement.RETURN_GENERATED_KEYS)){
            int c = mapPreparedStatement(user, statement,false);;
            if(c> 0){
                try(ResultSet set = statement.getGeneratedKeys()){
                    if(set.next()){
                        user.setId(set.getInt(1));
                    }
                }

            }
        } catch (SQLException e){
            logger.warn("Failed to insert new user with email: " + user.getEmail(), e);
            throw new MySQLException("Cannot insert new user" , e);
        }
    }


    /**
     Maps a User object to a PreparedStatement object for updating or adding the user in the database.
     @param user The User object containing the updated information.
     @param statement The PreparedStatement object to be mapped to the User object.
     @param update Is the statement an update
     @return The number of rows updated in the database.
     @throws SQLException If an error occurs while executing the SQL query.
     */
    private int mapPreparedStatement(User user, PreparedStatement statement, boolean update) throws SQLException {
        int i = 0;
        statement.setString(++i,user.getEmail());
        statement.setString(++i,user.getPassword());
        statement.setString(++i,user.getEntity());
        statement.setString(++i,user.getRole());
        statement.setString(++i,user.getFirstName());
        statement.setString(++i,user.getLastName());
        statement.setString(++i,user.getCompany());
        statement.setLong(++i,user.getTIN());
        statement.setString(++i,user.getCity());
        statement.setString(++i,user.getStreet());
        statement.setString(++i,user.getNumberOfBuilding());
        if(update)
            statement.setInt(++i,user.getId());
        return statement.executeUpdate();
    }

    /**
     * Gets a list of all users from the database.
     *
     * @return a list of all users
     * @throws MySQLException if an error occurs while interacting with the database
     */
    @Override
    public List<User> getAllUsers() throws MySQLException {
        try(Connection connection = ConnectionFactory.getConnection(true)){
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(MySQLQuery.SELECT_ALL_USERS);
            List<User> users = new ArrayList<>();
            while (rs.next()){
                User user = mapUser(rs);
                users.add(user);
            }
            return users;
        }catch (SQLException e){
            logger.warn("Failed to get a list of all users", e);
            throw new MySQLException("cannot select all users" , e);
        }
    }

    /**
     * Maps a ResultSet object to a User object.
     * @param rs ResultSet object from which the data is taken
     * @return A matched User object.
     * @throws SQLException if an error occurs while interacting with the database
     */
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("idUsers"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setEntity(rs.getString("entity"));
        user.setRole(rs.getString("role"));
        user.setFirstName(rs.getString("firstName"));
        user.setLastName(rs.getString("lastName"));
        user.setCompany(rs.getString("company"));
        user.setTIN(rs.getLong("TIN"));
        user.setCity(rs.getString("City"));
        user.setStreet(rs.getString("NumberOfBuilding"));
        return user;
    }

    /**
     * Gets a User  from the database.
     * @param email the email address of the user to retrieve
     * @return The user found
     * @ MySQLException if an error occurs while interacting with the database
     */
    @Override
    public User getByEmail(String email) throws MySQLException {
        try (Connection connection = ConnectionFactory.getConnection(true);
            PreparedStatement statement = connection.prepareStatement(MySQLQuery.SELECT_USER_BY_EMAIL)){
            statement.setString(1,email);
            ResultSet set = statement.executeQuery();
            set.next();
            logger.info("a user with the email address " + email + "was found ");
            return mapUser(set);
        } catch (SQLException e){
            logger.warn("Failed to get a user with email: " + email, e);
            throw new MySQLException("Cannot get user", e);
        }
    }

    /**
     * method hashes a given password using SHA-512 algorithm.
     * @param password the password to be hashed
     * @return hashes password
     */
    @Override
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update((password + "dd").getBytes());
            byte[] hash = digest.digest();
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e){
                throw new RuntimeException(e);
        }
    }

    /**
     Updates an existing user in the database.
     @param user the user to update
     @throws MySQLException if an error occurs while interacting with the database
     */
    @Override
    public void update(User user) throws MySQLException {
        Connection connection = null;
        try {
            connection = ConnectionFactory.getConnection(false);

            connection.commit();
            logger.info("User with email " + user.getEmail() + " was successfully updated");
        } catch (SQLException e) {
            ConnectionFactory.rollback(connection);
            logger.warn("Failed to update user with email: " + user.getEmail(), e);
            throw new MySQLException("Failed to update user", e);
        }finally {

        }
    }

    /**
     Updates a user in the database.
     @param user The User object containing the updated information.
     @param connection The Connection object used to connect to the database.
     @throws MySQLException If an error occurs while executing the SQL query.
     */
    private void updateUser(User user, Connection connection) throws MySQLException {
        try (PreparedStatement statement = connection.prepareStatement(MySQLQuery.UPDATE_USER_BY_EMAIL,Statement.RETURN_GENERATED_KEYS)){
            mapPreparedStatement(user, statement,true);
        } catch (SQLException e){
            logger.warn("Failed to update user with email: " + user.getEmail(), e);
            throw new MySQLException("Cannot update user with email: " + user.getEmail() , e);
        }
        logger.info("User with email " + user.getEmail() + " was successfully updated in the database.");
    }


    /**
     * Deletes a user from the database.
     *
     * @param id the ID of the user to delete
     * @throws MySQLException if a database access error occurs
     */
    @Override
    public void delete(int id) throws MySQLException {
        try (Connection connection = ConnectionFactory.getConnection(true);
            PreparedStatement statement = connection.prepareStatement(MySQLQuery.DELETE_USER)) {
            statement.setInt(1, id);
            int rowsDeleted = statement.executeUpdate();
            logger.info("Deleted {} row(s) from the users table", rowsDeleted);
        } catch (SQLException e) {
            logger.warn("failed to delete a user with ID: " + id, e);
            throw new MySQLException("Error deleting user from the database", e);
        }
    }
}