package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, TankModel tankModel) {
			Direction direction = fish.getDirection();
			if (direction == Direction.LEFT)
				endpoint.send(tankModel.getLeftNeighbour(), new HandoffRequest(fish));
			else if (direction == Direction.RIGHT)
				endpoint.send(tankModel.getRightNeighbour(), new HandoffRequest(fish));
		}

		public void sendToken(InetSocketAddress address) {
			endpoint.send(address, new Token());
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighborUpdate) {
					if (((NeighborUpdate) msg.getPayload()).getDirection() == Direction.LEFT)
						tankModel.setLeftNeighbour(((NeighborUpdate) msg.getPayload()).getAddress());
					else if (((NeighborUpdate) msg.getPayload()).getDirection() == Direction.RIGHT)
						tankModel.setRightNeighbour(((NeighborUpdate) msg.getPayload()).getAddress());
				}

				if (msg.getPayload() instanceof Token)
					tankModel.recieveToken();
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
