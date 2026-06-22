package com.projetok;

import com.esotericsoftware.kryonet.EndPoint;

public class Network {

    public static void register(EndPoint endPoint) {
        endPoint.getKryo().register(LoginRequest.class);
        endPoint.getKryo().register(LoginSuccess.class);
        endPoint.getKryo().register(LoginFailed.class);
        endPoint.getKryo().register(PlayerMovement.class);
        endPoint.getKryo().register(ChatMessage.class);
    }

    public static class LoginRequest {

        public String username;
        public String password;
    }

    public static class PlayerMovement {

        public float x, y;
    }

    public static class ChatMessage {

        public String text;
    }

    public static class LoginSuccess {
    }

    public static class LoginFailed {

        public String motivo;

        public LoginFailed() {
        }

        public LoginFailed(String motivo) {
            this.motivo = motivo;
        }
    }

}
