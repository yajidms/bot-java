package handlers;

import net.dv8tion.jda.api.JDA;

public class EventLoader {

    public static void loadEvents(JDA client) {
        try {
            // Events are already registered in Main.java through addEventListeners
            // This method can be used for additional event loading if needed
            System.out.println("Event handler loading! Events registered through Main.java");
        } catch (Exception e) {
            System.err.println("Error loading events: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
