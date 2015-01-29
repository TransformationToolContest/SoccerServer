/*
 * This file is part of the TTC 2014 SoccerServer.
 *
 * the TTC 2014 SoccerServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * the TTC 2014 SoccerServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * the TTC 2014 SoccerServer.  If not, see <http://www.gnu.org/licenses/>.
 */

package ttc14.soccerserver;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import soccerpitch.Ball;
import soccerpitch.Field;
import soccerpitch.GoalField;
import soccerpitch.Player;
import soccerpitch.Teams;

public class SwingSoccerPitchVisualizer extends JFrame implements
							   ISoccerPitchVisualizer {

    private static final long serialVersionUID = -133103613680895689L;
    private SoccerPitchController controller;

    private JLabel blueNameLabel = new JLabel("no client");
    private JLabel redNameLabel = new JLabel("no client");
    private JTextField blueScoreField = new JTextField(String.valueOf(0));
    private JTextField redScoreField = new JTextField(String.valueOf(0));

    @Override
    public void initialize(SoccerPitchController controller) {
	this.controller = controller;
	setDefaultCloseOperation(EXIT_ON_CLOSE);
	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(new JSoccerPitch(), BorderLayout.CENTER);
	JPanel bottomPanel = new JPanel();
	bottomPanel.setLayout(new GridLayout(1, 4));
	bottomPanel.add(blueNameLabel);
	blueScoreField.setEditable(false);
	bottomPanel.add(blueScoreField);
	bottomPanel.add(redNameLabel);
	redScoreField.setEditable(false);
	bottomPanel.add(redScoreField);
	getContentPane().add(bottomPanel, BorderLayout.SOUTH);
	pack();
	setResizable(false);
	setVisible(true);
    }

    private class JSoccerPitch extends JComponent {
	private static final int FIELD_SIZE = 20;
	private Font playerFont = new Font(Font.SERIF, Font.BOLD, FIELD_SIZE);

	@Override
	public Dimension getPreferredSize() {
	    return new Dimension(
				 coordToVizCoord(SoccerPitchController.PITCH_WIDTH),
				 coordToVizCoord(SoccerPitchController.PITCH_HEIGHT));
	}

	private static final long serialVersionUID = -6106372422628226558L;

	private void paintSoccerField(Graphics g, Field f) {
	    int vx = coordToVizCoord(f.getXPos());
	    int vy = coordToVizCoord(f.getYPos());
	    g.setColor(Color.GREEN);
	    g.fillRect(vx, vy, FIELD_SIZE, FIELD_SIZE);
	    g.setColor(Color.BLACK);
	    g.drawRect(vx, vy, FIELD_SIZE, FIELD_SIZE);
	}

	private void paintGoalField(Graphics g, GoalField gf) {
	    int vx = coordToVizCoord(gf.getXPos());
	    int vy = coordToVizCoord(gf.getYPos());
	    g.setColor(Color.WHITE);
	    g.fillRect(vx, vy, FIELD_SIZE, FIELD_SIZE);
	    g.setColor(gf.getTeam() == Teams.BLUE ? Color.BLUE : Color.RED);
	    g.drawRect(vx, vy, FIELD_SIZE, FIELD_SIZE);
	}

	private int coordToVizCoord(int x) {
	    return x * FIELD_SIZE;
	}

	@Override
	protected void paintComponent(Graphics g) {
	    super.paintComponent(g);
	    for (int y = 0; y < SoccerPitchController.PITCH_HEIGHT; y++) {
		for (int x = -1; x <= SoccerPitchController.PITCH_WIDTH; x++) {
		    Field f = controller.fieldAt(x, y);
		    if (f instanceof GoalField) {
			paintGoalField(g, (GoalField) f);
		    } else if (f != null) {
			paintSoccerField(g, f);
		    }
		}
	    }
	    paintBall(g, controller.getOrCreateSoccerPitch().getBall());
	    for (Player p : controller.getOrCreateSoccerPitch().getPlayers()) {
		paintPlayer(g, p);
	    }
	}

	private void paintBall(Graphics g, Ball ball) {
	    if (ball.getField() == null) {
		return;
	    }
	    int vx = coordToVizCoord(ball.getField().getXPos());
	    int vy = coordToVizCoord(ball.getField().getYPos());
	    g.setColor(Color.WHITE);
	    g.fillOval(vx, vy, FIELD_SIZE, FIELD_SIZE);
	    Color c;
	    Player p = ball.getPlayer();
	    if (p == null) {
		c = Color.BLACK;
	    } else if (p.getTeam() == Teams.BLUE) {
		c = Color.BLUE;
	    } else {
		c = Color.RED;
	    }
	    g.setColor(c);
	    Graphics2D g2d = (Graphics2D) g;
	    g2d.setStroke(new BasicStroke(2));
	    g2d.drawOval(vx, vy, FIELD_SIZE, FIELD_SIZE);
	}

	private void paintPlayer(Graphics g, Player p) {
	    if (p.getField() == null)
		return;
	    Color c = p.getTeam() == Teams.BLUE ? Color.BLUE : Color.RED;
	    int vx = coordToVizCoord(p.getField().getXPos()) + 3;
	    int vy = coordToVizCoord(p.getField().getYPos()) + FIELD_SIZE - 2;
	    g.setColor(c);
	    g.setFont(playerFont);
	    g.drawString(String.valueOf(p.getNumber()), vx, vy);
	}
    }

    @Override
    public void visualize() {
	String text = controller.getNameBLUE();
	blueNameLabel.setText(text == null ? "no client connected" : text);
	blueScoreField.setText(String.valueOf(controller.getScoreBLUE()));
	text = controller.getNameRED();
	redNameLabel.setText(text == null ? "no client connected" : text);
	redScoreField.setText(String.valueOf(controller.getScoreRED()));
	repaint();
    }

    @Override
    public void showResult() {
	JOptionPane.showMessageDialog(
				      this,
				      "The match finished " + controller.getNameBLUE() + "/BLUE "
				      + controller.getScoreBLUE() + ":"
				      + controller.getScoreRED() + " "
				      + controller.getNameRED() + "/RED.", "Match Result",
				      JOptionPane.INFORMATION_MESSAGE);
    }

}
