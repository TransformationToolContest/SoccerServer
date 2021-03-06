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

import soccerpitch.SoccerpitchPackage;
import updatemodel.UpdatemodelPackage;

public class SoccerServer {

    public static void main(String[] args) throws InterruptedException,
						  IOException {
	if (args.length != 3) {
	    System.err.println("Usage:");
	    System.err.println();
	    System.out
		.println("  java -jar SoccerServer.jar <PORT> <TURNS> <SLEEP_TIME>");
	    System.err.println();
	    System.err
		.println("<PORT>          The port the server listens on for clients.");
	    System.err
		.println("<TURNS>         The number of turns in a match.");
	    System.err
		.println("<SLEEP_TIME>    The pause time (in ms) between player actions.");
	    return;
	}
	SoccerPitchController.configPort = Integer.parseInt(args[0]);
	SoccerPitchController.configGameTurns = Integer.parseInt(args[1]);
	SoccerPitchController.configSleepTime = Integer.parseInt(args[2]);

	SoccerpitchPackage.eINSTANCE.eContents();
	UpdatemodelPackage.eINSTANCE.eContents();

	SoccerPitchController controller = new SoccerPitchController();
	controller.awaitClients();
	controller.kickoff();
    }
}
