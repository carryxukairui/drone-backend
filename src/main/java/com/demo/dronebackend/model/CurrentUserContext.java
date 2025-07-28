package com.demo.dronebackend.model;


import com.demo.dronebackend.pojo.User;

public class CurrentUserContext {
    private static final ThreadLocal<User> holder = new ThreadLocal<>();

    public static void set(User user) {
        holder.set(user);
    }

    public static User get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
