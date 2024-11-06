package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
	private final TankModel tankModel;

	public SnapshotController(TankModel tankModel) {
        this.tankModel = tankModel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tankModel.initiateSnapshot();

		//wait for snapshot to finish
		while(tankModel.snapshotInProgress) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		JOptionPane.showMessageDialog(null, "Snapshotcount: " + this.tankModel.globalSnapshotCounter, "Snapshot finished", JOptionPane.INFORMATION_MESSAGE);
	}
}
