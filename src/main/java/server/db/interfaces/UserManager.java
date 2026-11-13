package server.db.interfaces;

import commonInfo.Role;
import commonInfo.User;

import java.util.List;

public interface UserManager {
        public void newUser(User user);
        public List<User> getUsers();
        public User checkPassword(String email, String pass);
        public void changePassword(User user, String new_passwd);
        public String getUserMailFromId(Integer id);
        public User getUserById(Integer id);
        public void deleteUserById(int id);
        public void deleteUser(User user);

        public void getUserByEmail(String email);
        public List<User> getUsersByRole(Role role);
}
