package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class UserManager extends EntityManager<User> {

    @Override
    protected Class<User> entityClass() {
        return User.class;
    }

    @Override
    public String getLog(User user) throws SQLException, ReflectiveOperationException, ManagerException {
        return user.getUsername() != null ? user.getUsername() : "" + user.getId();
    }

    public User authenticate(String username, String password) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(User.class)).append(" u ")
                .append("join _credential c on c.user_id = u.id ")
                .append("where u.username = '").append(username).append("' ")
                .append("and c.password = upper(sha1('").append(password).append("')) ")
                .append("and status = 1");
        return domainManager.loadUnique(User.class, sql.toString());
    }

    public void setPassword(User user, String password) throws ManagerException, SQLException {
        if (!checkPassword(password)) {
            throw new ManagerException("invalid password");
        }
        String sql = "update _credential set password = upper(sha1('" + password + "')) where user_id = " + user.getId();
        if (databaseManager.update(sql) == 0) {
            sql = "insert into _credential values (" + user.getId() + ", upper(sha1('" + password + "')))";
            databaseManager.update(sql);
        }
    }

    private boolean checkPassword(String password) {
        return password != null && password.length() >= 8;
    }

}
