package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.FolderManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.manager.UserManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("user")
public class UserController extends EntityController<User> {

    @Autowired
    private UserManager userManager;
    @Autowired
    private FolderManager folderManager;

    @Override
    protected Class<User> entityClass() {
        return User.class;
    }

    @Override
    protected EntityManager<User> entityManager() {
        return userManager;
    }

    @Override
    protected void checkAccess(User user, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (Action.REMOVE.equals(action) && accessManager.contains(user)) {
            throw new ManagerException("unable to remove connected user");
        }
        accessManager.checkAccess(user, action);
    }

    @GetMapping("me")
    public User me() throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        User me = accessManager.getUser();
        logManager.setMessage("get me").appendMessage(User.class.getSimpleName()).appendMessage(entityManager().getLog(me));
        accessManager.checkAccess(me, Action.READ);
        return me;
    }

    @PostMapping("{id}/password")
    public void password(@PathVariable("id") long id, @RequestBody String password) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("set password of").appendMessage(User.class.getSimpleName());
        User user = userManager.load(id);
        if (user == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(user));
        accessManager.checkAccess(user, Action.WRITE);
        userManager.setPassword(user, password);
    }

    @PostMapping("password")
    public void password(@RequestBody String password) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        User me = accessManager.getUser();
        logManager.setMessage("set password of").appendMessage(User.class.getSimpleName()).appendMessage(entityManager().getLog(me));
        accessManager.checkAccess(me, Action.WRITE);
        userManager.setPassword(me, password);
    }

    @GetMapping("{id}/home")
    public Folder home(@PathVariable("id") long id) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get home of").appendMessage(User.class.getSimpleName());
        User user = userManager.load(id);
        if (user == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(user));
        Folder home = folderManager.getHome(user);
        accessManager.checkAccess(home, Action.READ);
        return home;
    }

    @GetMapping("home")
    public Folder home() throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        User me = accessManager.getUser();
        logManager.setMessage("get home of " + User.class.getSimpleName()).appendMessage(entityManager().getLog(me));
        Folder home = folderManager.getHome(me);
        accessManager.checkAccess(home, Action.READ);
        return home;
    }

    @GetMapping("{id}/tree")
    public List<Map<String, Object>> tree(@PathVariable("id") long id) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get tree of").appendMessage(User.class.getSimpleName());
        User user = userManager.load(id);
        if (user == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(user));
        Folder home = folderManager.getHome(user);
        accessManager.checkAccess(home, Action.READ);
        return folderManager.tree(home);
    }

    @GetMapping("tree")
    public List<Map<String, Object>> tree() throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        User me = accessManager.getUser();
        logManager.setMessage("get tree of " + User.class.getSimpleName()).appendMessage(entityManager().getLog(me));
        Folder home = folderManager.getHome(me);
        accessManager.checkAccess(home, Action.READ);
        return folderManager.tree(home);
    }

}
