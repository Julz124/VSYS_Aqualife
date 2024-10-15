package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt2.broker.PoisonPill;
import aqua.blatt2.broker.Poisoner;
import messaging.Endpoint;
import messaging.Message;

// Message Types
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Broker {
    private Endpoint endpoint;
    private ClientCollection<InetSocketAddress> client_list;
    private int client_count = 0;
    private static ExecutorService thread_pool = Executors.newFixedThreadPool(8);
    private static volatile boolean stopRequested = false;

    public Broker() {
        this.endpoint = new Endpoint(4711);
        this.client_list = new ClientCollection<>();
    }

    private class BrokerTask implements Runnable {
        private Message msg;

        public BrokerTask(Message msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            if (msg.getPayload() instanceof RegisterRequest) {
                register(msg);
            }
            if (msg.getPayload() instanceof DeregisterRequest) {
                deregister(msg);
            }
            if (msg.getPayload() instanceof HandoffRequest) {
                handoffFish(msg);
            }
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
            FishModel fish = ((HandoffRequest) msg.getPayload()).getFish();
            if (fish.getDirection() == Direction.LEFT) {
                endpoint.send(client_list.getLeftNeighorOf(curr_client_index), new HandoffRequest(((HandoffRequest) msg.getPayload()).getFish()));
            } else {
                endpoint.send(client_list.getRightNeighorOf(curr_client_index), new HandoffRequest(((HandoffRequest) msg.getPayload()).getFish()));
            }
        }
    }

    private void broker() {

        new Thread(() -> Poisoner.main(null)).start();

        Message msg;
        while(true) {
            msg = endpoint.blockingReceive();

            if (msg.getPayload() instanceof PoisonPill) {
                for (int i = 0; i < client_list.size(); i++){
                    endpoint.send(client_list.getClient(i), new PoisonPill());
                }

                stopRequested = true;
                break;
            }
            thread_pool.execute(new BrokerTask(msg));
        }
        thread_pool.shutdown();
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }
}


