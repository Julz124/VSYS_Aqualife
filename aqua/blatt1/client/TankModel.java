package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;

	protected InetSocketAddress left_neighbour = null;
	protected InetSocketAddress right_neighbour = null;

	protected boolean token = false;
	protected Timer timer = new Timer();

	protected SnapshotState snapshotState = SnapshotState.IDLE;
	boolean snapshotInProgress = false;
	boolean isInitializer = false;
	int localSnapshotCounter = 0;
	int globalSnapshotCounter = 0;
	int fadingFishiesCounter = 0;

	enum SnapshotState {
		IDLE,
		LEFT,
		RIGHT,
		BOTH
	}

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge() && token)
				forwarder.handOff(fish, this);
				fadingFishiesCounter++;
			if (fish.hitsEdge() && !token)
				fish.reverse();

			if (fish.disappears())
				it.remove();
				fadingFishiesCounter--;
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public synchronized void neighbourUpdate(InetSocketAddress left_neighbour, InetSocketAddress right_neighbour) {
		this.left_neighbour = left_neighbour;
		this.right_neighbour = right_neighbour;
	}

	public synchronized void setLeftNeighbour(InetSocketAddress left_neighbour) { this.left_neighbour = left_neighbour; }
	public synchronized void setRightNeighbour(InetSocketAddress right_neighbour) { this.right_neighbour = right_neighbour; }
	public synchronized InetSocketAddress getLeftNeighbour() { return this.left_neighbour; }
	public synchronized InetSocketAddress getRightNeighbour() { return this.right_neighbour; }

	public synchronized void recieveToken() {
		this.token = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				sendToken();
			}
		};
		timer.schedule(task, 2000);
	}

	public synchronized boolean hasToken() { return this.token; }

	private synchronized void sendToken() {
		token = false;
		forwarder.sendToken(left_neighbour);
	}

	public synchronized void initiateSnapshot() {
		snapshotState = SnapshotState.BOTH;
		snapshotInProgress = true;

		this.isInitializer = true;
		this.localSnapshotCounter = fishies.size() - fadingFishiesCounter;

		forwarder.sendSnapshotMarker(left_neighbour);
		forwarder.sendSnapshotMarker(right_neighbour);
	}

	public synchronized void recieveSnapshotMarker(Direction dir) {
		//case idle
		if(this.snapshotState.equals(SnapshotState.IDLE)) {

			this.localSnapshotCounter = fishies.size() - fadingFishiesCounter;

			if(dir.equals(Direction.LEFT)){
				this.snapshotState = SnapshotState.RIGHT;
				forwarder.sendSnapshotMarker(right_neighbour);
			}else{
				this.snapshotState = SnapshotState.LEFT;
				forwarder.sendSnapshotMarker(left_neighbour);
			}

			// case both
		} else if(this.snapshotState.equals(SnapshotState.BOTH)) {
			if(dir.equals(Direction.LEFT)){
				this.snapshotState = SnapshotState.RIGHT;
			}else{
				this.snapshotState = SnapshotState.LEFT;
			}

		} else {
			this.snapshotState = SnapshotState.IDLE;
			if(!this.isInitializer) {
				forwarder.sendSnapshotMarker(dir.equals(Direction.LEFT) ? right_neighbour : left_neighbour);
				System.out.println("Snapshot complete (Non-Initializer), Fishcount: " + this.localSnapshotCounter);
			} else {
				//Snapshot complete
				//this.isInitializer = false;
				forwarder.sendSnapshotToken(new SnapshotToken(), left_neighbour);
				System.out.println("Snapshot complete (Initializer), Fishcount: " + this.localSnapshotCounter);

			}
		}
	}

	public synchronized void recieveSnapshotToken(SnapshotToken token) {
		System.out.println(token.getGlobalCounter());
		if (!this.isInitializer) {
			token.addGlobalCounter(this.localSnapshotCounter);
			forwarder.sendSnapshotToken(token, left_neighbour);
		} else if (this.isInitializer) {
			token.addGlobalCounter(this.localSnapshotCounter);
			this.globalSnapshotCounter = token.getGlobalCounter();
			this.snapshotInProgress = false;
			this.isInitializer = false;
		}
	}

}