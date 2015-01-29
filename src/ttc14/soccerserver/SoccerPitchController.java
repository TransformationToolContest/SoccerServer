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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;

import javax.swing.SwingUtilities;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import soccerpitch.Ball;
import soccerpitch.Field;
import soccerpitch.GoalField;
import soccerpitch.GoalKeeper;
import soccerpitch.Player;
import soccerpitch.SoccerPitch;
import soccerpitch.SoccerpitchFactory;
import soccerpitch.Teams;
import updatemodel.Action;
import updatemodel.MovePlayer;
import updatemodel.ShootBall;
import updatemodel.Update;

public class SoccerPitchController {

    public static final int PITCH_HEIGHT = 25;
    public static final int PITCH_WIDTH = 45;
    public static final int GOAL_LENGTH = 7;
    public static final double BALL_INTERCEPTION_DIST = 1.0;
    public static final int MAX_MOVE_DIST = 3;
    public static final int MAX_MOVE_DIST_WITH_BALL = 2;
    public static final int MAX_SHOOT_DIST = 7;
    public static final int MAX_SHOOTS = 4;

    static int configPort = 55555;
    static int configGameTurns = 1000;
    static int configSleepTime = 500;

    private SoccerPitch pitch;
    private UpdateEvaluator evaluator = new UpdateEvaluator();
    private XMIResource resource;
    private int scoreBLUE = 0;
    private String nameBLUE;
    private ISoccerPitchVisualizer visualizer;
    private Teams goal = null;
    private ServerSocket serverSocket;

    SoccerPitchController() throws IOException {
	serverSocket = new ServerSocket(configPort);
	getOrCreateSoccerPitch();
	visualizer = new SwingSoccerPitchVisualizer();
	visualizer.initialize(this);
	visualizer.visualize();
    }

    String getNameBLUE() {
	return nameBLUE;
    }

    void setNameBLUE(String nameBLUE) {
	this.nameBLUE = nameBLUE;
    }

    String getNameRED() {
	return nameRED;
    }

    void setNameRED(String nameRED) {
	this.nameRED = nameRED;
    }

    private int scoreRED = 0;

    int getScoreBLUE() {
	return scoreBLUE;
    }

    int getScoreRED() {
	return scoreRED;
    }

    private String nameRED;
    private ClientHandler clientHandlerBLUE;
    private ClientHandler clientHandlerRED;

    private void createFields() {
	int gap = (PITCH_HEIGHT - GOAL_LENGTH) / 2;
	for (int x = 0; x < PITCH_WIDTH; x++) {
	    for (int y = 0; y < PITCH_HEIGHT; y++) {
		Field f;
		if ((x == 0 || x == PITCH_WIDTH - 1) && y >= gap
		    && y < PITCH_HEIGHT - gap) {
		    f = SoccerpitchFactory.eINSTANCE.createGoalField();
		    if (x == 0) {
			((GoalField) f).setTeam(Teams.BLUE);
		    } else {
			((GoalField) f).setTeam(Teams.RED);
		    }
		} else {
		    f = SoccerpitchFactory.eINSTANCE.createField();
		}
		pitch.getFields().add(f);
		f.setXPos(x);
		f.setYPos(y);
		if (x >= 1) {
		    Field of = fieldAt(x - 1, y);
		    f.setWest(of);
		}
		if (y >= 1) {
		    Field of = fieldAt(x, y - 1);
		    f.setNorth(of);
		}
	    }

	}
    }

    private void createSoccerPitch() {
	resource = new XMIResourceImpl();
	pitch = SoccerpitchFactory.eINSTANCE.createSoccerPitch();
	resource.getContents().add(pitch);
	createFields();
	createPlayers();
	createBall();
    }

    Player randomFieldPlayer() {
	Player p = null;
	do {
	    int n = new Random().nextInt(pitch.getPlayers().size());
	    p = pitch.getPlayers().get(n);
	} while (p instanceof GoalKeeper);
	return p;
    }

    Player randomFieldPlayer(Teams team) {
	Player p = null;
	do {
	    int n = new Random().nextInt(pitch.getPlayers().size());
	    p = pitch.getPlayers().get(n);
	} while (p instanceof GoalKeeper || p.getTeam() != team);
	return p;
    }

    private void createBall() {
	Ball ball = SoccerpitchFactory.eINSTANCE.createBall();
	pitch.setBall(ball);
    }

    private void placePlayersRow(int count, int x, int firstNumber) {
	int gap = PITCH_HEIGHT / (count + 1);
	int pos = gap;
	int number = firstNumber;

	for (int i = 0; i < count; i++) {
	    Player blueP = getPlayer(number, Teams.BLUE);
	    if (blueP != null)
		blueP.setField(fieldAt(x, pos));

	    Player redP = getPlayer(firstNumber + count - i - 1, Teams.RED);
	    if (redP != null)
		redP.setField(fieldAt(PITCH_WIDTH - x - 1, pos));

	    pos += gap;
	    number++;
	}
    }

    private void createPlayers() {
	GoalKeeper blueKeeper = SoccerpitchFactory.eINSTANCE.createGoalKeeper();
	blueKeeper.setTeam(Teams.BLUE);
	blueKeeper.setNumber(1);
	pitch.getPlayers().add(blueKeeper);

	GoalKeeper redKeeper = SoccerpitchFactory.eINSTANCE.createGoalKeeper();
	redKeeper.setTeam(Teams.RED);
	redKeeper.setNumber(1);
	pitch.getPlayers().add(redKeeper);

	for (int i = 2; i < 10; i++) {
	    Player blueP = SoccerpitchFactory.eINSTANCE.createFieldPlayer();
	    blueP.setTeam(Teams.BLUE);
	    blueP.setNumber(i);
	    pitch.getPlayers().add(blueP);

	    Player redP = SoccerpitchFactory.eINSTANCE.createFieldPlayer();
	    redP.setTeam(Teams.RED);
	    redP.setNumber(i);
	    pitch.getPlayers().add(redP);
	}

	placePlayersForKickoff();
    }

    private void placePlayersForKickoff() {
	placePlayersRow(1, 0, 1);
	placePlayersRow(2, 5, 2);
	placePlayersRow(3, 10, 4);
	placePlayersRow(2, 15, 7);
	placePlayersRow(1, 20, 9);
    }

    SoccerPitch getOrCreateSoccerPitch() {
	if (pitch == null) {
	    createSoccerPitch();
	}
	return pitch;
    }

    XMIResource getSoccerPitchResource() {
	return resource;
    }

    private Player getPlayer(int number, Teams team) {
	for (Player p : pitch.getPlayers()) {
	    if (p.getNumber() == number && p.getTeam() == team) {
		return p;
	    }
	}
	return null;
    }

    Field fieldAt(int x, int y) {
	Field f = pitch.getFields().get(0);
	if (f.getXPos() != 0 || f.getYPos() != 0) {
	    throw new RuntimeException();
	}
	for (int i = y; i > 0; i--) {
	    f = f.getSouth();
	}
	if (x >= 0) {
	    for (int i = x; i > 0; i--) {
		f = f.getEast();
	    }
	} else {
	    f = f.getWest();
	}
	if (f != null && (f.getXPos() != x || f.getYPos() != y)) {
	    throw new RuntimeException("Wanted (" + x + ", " + y
				       + ") but got (" + f.getXPos() + ", " + f.getYPos() + ")");
	}
	return f;
    }

    private void giveBallToPlayer(Player receiver) {
	Ball ball = pitch.getBall();
	receiver.setBall(ball);
	ball.setField(receiver.getField());
    }

    class UpdateEvaluator {
	HashSet<Player> actionPlayers;
	int noOfShoots = 0;

	private void reset() {
	    actionPlayers = new HashSet<Player>();
	    noOfShoots = 0;
	}

	void evalAction(Action a, final Teams currentTeam) {
	    Player p = getPlayer(a.getPlayerNumber(), currentTeam);
	    if (p == null) {
		// That player has already been deleted.
		return;
	    }
	    if (actionPlayers.contains(p)) {
		System.err
		    .println("Player "
			     + p.getNumber()
			     + " of team "
			     + currentTeam
			     + " already did his action. Skipping second action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }
	    actionPlayers.add(p);
	    if (a instanceof MovePlayer) {
		evalMovePlayer((MovePlayer) a, currentTeam);
	    } else if (a instanceof ShootBall) {
		evalShootBall((ShootBall) a, currentTeam);
	    }
	    visualizer.visualize();
	    try {
		Thread.sleep(configSleepTime);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	private boolean validDistance(int distX, int distY, int max) {
	    return Math.abs(distX) <= max && Math.abs(distY) <= max;
	}

	private Field targetField(Field start, int distX, int distY) {
	    int endX = start.getXPos() + distX;
	    int endY = start.getYPos() + distY;
	    if (endX >= PITCH_WIDTH)
		endX = PITCH_WIDTH - 1;
	    if (endX < 0)
		endX = 0;
	    if (endY >= PITCH_HEIGHT)
		endY = PITCH_HEIGHT - 1;
	    if (endY < 0)
		endY = 0;
	    return fieldAt(endX, endY);
	}

	private void evalShootBall(ShootBall a, final Teams currentTeam) {
	    noOfShoots++;
	    Player p = getPlayer(a.getPlayerNumber(), currentTeam);
	    if (p == null) {
		return;
	    }
	    if (noOfShoots > MAX_SHOOTS) {
		System.err
		    .println("Player "
			     + p.getNumber()
			     + " of team "
			     + currentTeam
			     + " wants to shoot but the max number of shoots per turn has already been reached. Skipping action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }
	    if (p.getBall() == null) {
		System.err
		    .println("Player "
			     + a.getPlayerNumber()
			     + " of team "
			     + currentTeam
			     + " doesn't have the ball so cannot shoot it. Skipping action!");
		return;
	    }

	    if (!validDistance(a.getXDist(), a.getYDist(), MAX_SHOOT_DIST)) {
		System.err
		    .println("Player "
			     + a.getPlayerNumber()
			     + " of team "
			     + currentTeam
			     + " wants to shoot an too large distance. Skipping action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }

	    Field target = targetField(p.getField(), a.getXDist(), a.getYDist());

	    if (target == null) {
		System.err
		    .println("Player "
			     + p.getNumber()
			     + " of team "
			     + currentTeam
			     + " wants to shoot to an invalid location. Skipping action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }

	    System.out.println("Player " + a.getPlayerNumber() + " of team "
			       + currentTeam + " shoots the ball.");
	    Player interceptingPlayer = findInterceptingPlayer(a, currentTeam,
							       p, target, true);
	    if (interceptingPlayer != null && Math.random() < 0.3) {
		System.out.println("Player " + interceptingPlayer.getNumber()
				   + " of team " + interceptingPlayer.getTeam()
				   + " intercepts the ball.");
		giveBallToPlayer(interceptingPlayer);
		return;
	    }
	    Ball ball = pitch.getBall();
	    ball.setField(target);
	    ball.setPlayer(null);

	    if (target instanceof GoalField) {
		GoalField goalField = (GoalField) target;
		maybeGoal(goalField);
	    } else {
		if (!target.getPlayers().isEmpty()) {
		    Player receiver = target.getPlayers().get(
							      new Random().nextInt(target.getPlayers().size()));
		    giveBallToPlayer(receiver);
		    System.out.println("Player " + receiver.getNumber()
				       + " of team " + receiver.getTeam()
				       + " receives the ball.");
		}
	    }
	}

	private Player findInterceptingPlayer(Action action,
					      final Teams currentTeam, Player currentPlayer, Field target,
					      boolean excludeTargetPlayers) {
	    int startX = currentPlayer.getField().getXPos();
	    int startY = currentPlayer.getField().getYPos();
	    int endX = target.getXPos();
	    int endY = target.getYPos();
	    Player interceptingPlayer = null;
	    double m = 1.0 * action.getYDist() / action.getXDist();
	    double n = 1.0 * startY - m * startX;
	    double minDist = Double.MAX_VALUE;
	    for (Player op : pitch.getPlayers()) {
		if (op.getTeam() == currentTeam) {
		    continue;
		}
		if (excludeTargetPlayers && op.getField() == target) {
		    continue;
		}
		int opX = op.getField().getXPos();
		int opY = op.getField().getYPos();
		int minX = startX < endX ? startX : endX;
		int minY = startY < endY ? startY : endY;
		int maxX = startX > endX ? startX : endX;
		int maxY = startY > endY ? startY : endY;
		if (opX >= minX && opX <= maxX && opY >= minY && opY <= maxY) {
		    double distance = Math.abs((m * opX - opY + n)
					       / Math.sqrt(m * m + 1));
		    if (distance < minDist) {
			minDist = distance;
			interceptingPlayer = op;
		    }
		}
	    }
	    if (minDist < BALL_INTERCEPTION_DIST) {
		return interceptingPlayer;
	    }
	    return null;
	}

	private void maybeGoal(GoalField goalField) {
	    if (pitch.getBall().getField() != goalField) {
		throw new RuntimeException();
	    }
	    Teams goalOfTeam = goalField.getTeam();
	    if (pitch.getBall().getPlayer() != null
		&& pitch.getBall().getPlayer().getTeam() == goalOfTeam) {
		return;
	    }
	    for (Player p : goalField.getPlayers()) {
		if (p instanceof GoalKeeper && Math.random() < 0.7) {
		    giveBallToPlayer(p);
		    System.out.println("The " + goalOfTeam
				       + " goal keeper catches the ball!");
		    return;
		}
		if (p.getTeam() == goalOfTeam && Math.random() < 0.2) {
		    giveBallToPlayer(p);
		    System.out.println("Player " + p.getNumber() + " of team "
				       + goalOfTeam + " rescues the ball.");
		    return;
		}
	    }
	    if (goalField.getTeam() == Teams.BLUE) {
		System.out.println("Team " + Teams.RED + " scores a goal!!!");
		scoreRED++;
		goal = Teams.RED;
	    } else {
		System.out.println("Team " + Teams.BLUE + " scores a goal!!!");
		scoreBLUE++;
		goal = Teams.BLUE;
	    }
	}

	private void evalMovePlayer(MovePlayer mp, final Teams currentTeam) {
	    Player p = getPlayer(mp.getPlayerNumber(), currentTeam);
	    if (p == null) {
		return;
	    }

	    if (!validDistance(mp.getXDist(), mp.getYDist(),
			       p.getBall() == null ? MAX_MOVE_DIST
			       : MAX_MOVE_DIST_WITH_BALL)) {
		System.err.println("Player " + mp.getPlayerNumber()
				   + " of team " + currentTeam
				   + " wants to move further than allowed ("
				   + mp.getXDist() + ", " + mp.getYDist()
				   + "). Skipping action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }

	    Field target = targetField(p.getField(), mp.getXDist(),
				       mp.getYDist());

	    if (p instanceof GoalKeeper && !(target instanceof GoalField)) {
		System.out
		    .println("The goal keeper of team "
			     + currentTeam
			     + " wants to leave his goal. Skipping action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }

	    if (target == null) {
		System.err
		    .println("Player "
			     + p.getNumber()
			     + " of team "
			     + currentTeam
			     + " wants to move to an invalid location. Skipping action and removing him!");
		EcoreUtil.delete(p);
		return;
	    }

	    if (p.getBall() != null) {
		// Player with ball: just move, and maybe the ball will be
		// intercepted.
		Player interceptingPlayer = findInterceptingPlayer(mp,
								   currentTeam, p, target, false);
		if (interceptingPlayer != null && Math.random() < 0.3) {
		    System.out.println("Player "
				       + interceptingPlayer.getNumber() + " of team "
				       + interceptingPlayer.getTeam()
				       + " intercepts the ball from player "
				       + p.getNumber() + " of the " + p.getTeam()
				       + " team.");
		    giveBallToPlayer(interceptingPlayer);
		} else {
		    Ball ball = p.getBall();
		    ball.setField(target);
		}
		p.setField(target);
		if (target instanceof GoalField && p.getBall() != null) {
		    maybeGoal((GoalField) target);
		}
	    } else {
		// Player without ball: just move, and maybe attack the other
		// team's player at target location.
		p.setField(target);
		Ball ball = pitch.getBall();
		if (ball.getField() == target) {
		    if (ball.getPlayer() == null) {
			ball.setPlayer(p);
			System.out.println("Player " + p.getNumber()
					   + " of team " + p.getTeam()
					   + " captured the ball.");
		    } else if (ball.getPlayer().getTeam() != p.getTeam()
			       && !(ball.getPlayer() instanceof GoalKeeper)
			       && Math.random() < 0.5) {
			System.out.println("Player " + p.getNumber()
					   + " of team " + p.getTeam()
					   + " captured the ball from player "
					   + ball.getPlayer().getNumber() + " of the "
					   + ball.getPlayer().getTeam() + " team.");
			ball.setPlayer(p);
		    }
		}
	    }
	}

	void evaluateUpdate(Update update, final Teams currentTeam) {
	    reset();
	    if (update == null) {
		System.err.println("Received model contained no update!");
		return;
	    }
	    for (final Action a : update.getActions()) {
		try {
		    SwingUtilities.invokeAndWait(new Runnable() {
			    @Override
			    public void run() {
				evalAction(a, currentTeam);
			    }
			});
		} catch (InvocationTargetException e) {
		    e.printStackTrace();
		    throw new RuntimeException(e);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		    throw new RuntimeException(e);
		}
	    }
	}

    }

    void awaitClients() throws IOException {
	Socket blueSocket = serverSocket.accept();
	clientHandlerBLUE = new ClientHandler(this, blueSocket, Teams.BLUE);
	visualizer.visualize();

	Socket redSocket = serverSocket.accept();
	clientHandlerRED = new ClientHandler(this, redSocket, Teams.RED);
	visualizer.visualize();
    }

    void kickoff() throws InterruptedException {
	int turns = 0;
	outer: while (true) {
	    placePlayersForKickoff();
	    Teams wb = (goal == Teams.BLUE) ? Teams.RED
		: (goal == Teams.RED) ? Teams.BLUE
		: (Math.random() < 0.5) ? Teams.BLUE : Teams.RED;
	    for (int i = 1; i <= 9; i++) {
		Player p = getPlayer(i, wb);
		if (p != null) {
		    giveBallToPlayer(p);
		    break;
		}
	    }
	    visualizer.visualize();
	    goal = null;
	    ClientHandler withBall = pitch.getBall().getPlayer().getTeam() == Teams.BLUE ? clientHandlerBLUE
		: clientHandlerRED;
	    ClientHandler withoutBall = withBall == clientHandlerBLUE ? clientHandlerRED
		: clientHandlerBLUE;
	    System.out
		.println("The " + withBall.getTeam() + " team kicks off.");
	    while (turns < configGameTurns || (scoreBLUE - scoreRED == 0)) {
		turns++;
		Update update = withBall.sendPitchAndReceiveUpdate();
		evaluator.evaluateUpdate(update, withBall.getTeam());
		visualizer.visualize();
		if (goal != null) {
		    continue outer;
		}
		update = withoutBall.sendPitchAndReceiveUpdate();
		evaluator.evaluateUpdate(update, withoutBall.getTeam());
		if (goal != null) {
		    continue outer;
		}
		visualizer.visualize();
	    }
	    break outer;
	}
	System.out.println("The match finishes " + scoreBLUE + ":" + scoreRED);
	visualizer.showResult();
	clientHandlerBLUE.terminate();
	clientHandlerRED.terminate();
    }
}
