package server.db.interfaces;

import commonInfo.Role;
import commonInfo.User;

import java.util.List;

public interface UserManager {
        public void newUser(User user);
        public List<User> getUsers();
        public User checkPassword(String email, String pass);
        public void changePassword(User user, String newPassword);
        public String getUserMailFromId(Integer id);
        public User getUserById(Integer id);
        public void deleteUserById(int id);
        public void deleteUser(User user);
        public User getUserByEmail(String email);
        public List<User> getUsersByRole(Role role);
}
