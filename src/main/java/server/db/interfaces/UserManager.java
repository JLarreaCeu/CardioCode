package server.db.interfaces;

import commonInfo.Role;
import commonInfo.User;

import java.util.List;

public interface UserManager {
        public void connect();
        public void newRole(Role role);
        public void newUser(User user);
        public List<Role> getRoles();
        public List<User> getUsers();
        public Role getRole(Integer id);
        public User checkPassword(String email, String pass);
        public void changePassword(User user, String new_passwd);
        public String getUserMailFromId(Integer id);
        public User getUserByEmail(String email);
        public User getUserById(Integer id);
        public void deleteUserById(int id);
        public void deleteUser(User user);
        public List<User> getListOfUsersByRoleId(Integer role_id);

}
