package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;

// Message Types
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;

import java.net.InetSocketAddress;

public class Broker {
    Endpoint endpoint;

    ClientCollection<InetSocketAddress> client_list;

    int client_count = 0;

    public Broker() {
        this.endpoint = new Endpoint(4711);
        this.client_list = new ClientCollection<>();
    }

    private void register(Message msg) {
        InetSocketAddress curr_client = null;
        try {
            curr_client = client_list.getClient(client_list.indexOf(msg.getSender()));
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Client list is Empty");
        }

        if (curr_client != null) {
            System.out.println("Client " + curr_client + " already registered");
        } else {
            client_list.add("tank" + client_count, msg.getSender());
            System.out.println("Client " + msg.getSender() + " got registered as tank" + client_count);
            endpoint.send(msg.getSender(), new RegisterResponse("tank" + client_count));
            client_count++;
        }
    }

    private void deregister(Message msg) {
        int curr_client_index = client_list.indexOf(msg.getSender());
        client_list.remove(curr_client_index);
        System.out.println("Client tank" + curr_client_index + " got succesfully removed");
    }

    private void handoffFish(Message msg) {
        int curr_client_index = client_list.indexOf(msg.getSender());
        int next_client_index = (curr_client_index + 1) % client_list.size();

        System.out.println("Next hand off Client is tank" + next_client_index);
        endpoint.send(client_list.getClient(next_client_index), new HandoffRequest(((HandoffRequest) msg.getPayload()).getFish()));

    }

    public void broker() {
        Message message;
        while(true) {

            message = endpoint.blockingReceive();

            if (message.getPayload() instanceof RegisterRequest) {
                register(message);
            }
            if (message.getPayload() instanceof DeregisterRequest) {
                deregister(message);
            }
            if (message.getPayload() instanceof HandoffRequest) {
                handoffFish(message);
            }
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }
}


