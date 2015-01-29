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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import soccerpitch.Teams;
import updatemodel.Update;

public class ClientHandler {
	public static final String START_MARKER = "#START_RESOURCE#";
	public static final String END_MARKER = "#END_RESOURCE#";

	private SoccerPitchController controller;
	private Socket socket;
	private Teams team;

	public Teams getTeam() {
		return team;
	}

	void terminate() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private PrintWriter out;
	private BufferedReader in;

	public ClientHandler(SoccerPitchController controller, Socket socket,
			Teams team) throws IOException {
		System.out.println("Client for team " + team + " connected!");
		this.controller = controller;
		this.socket = socket;
		this.team = team;
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out.println(team);
		out.flush();
		String name = in.readLine();
		if (this.team == Teams.BLUE)
			this.controller.setNameBLUE(name);
		else
			this.controller.setNameRED(name);
	}

	private void sendResource(XMIResource r) {
		// out is new PrintWriter(new
		// OutputStreamWriter(socket.getOutputStream()));
		out.println(START_MARKER);
		try {
			Map<Object, Object> opts = r.getDefaultSaveOptions();
			opts.put(XMLResource.OPTION_FORMATTED, false);
			r.save(out, opts);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		out.println(END_MARKER);
		out.flush();
	}

	private XMIResource receiveResource() {
		// in is new BufferedReader(new
		// InputStreamReader(socket.getInputStream()));
		try {
			String s = in.readLine();
			if (s == null) {
				throw new RuntimeException(team + " client lost connection.");
			}
			if (!s.equals(START_MARKER)) {
				throw new RuntimeException("Bad message from " + team
						+ " client.");
			}
			StringBuilder sb = new StringBuilder();
			while (!(s = in.readLine()).equals(END_MARKER)) {
				sb.append(s);
			}
			s = sb.toString();
			XMIResource r = new XMIResourceImpl();
			r.load(new ByteArrayInputStream(s.getBytes()),
					r.getDefaultLoadOptions());
			return r;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Update sendPitchAndReceiveUpdate() {
		XMIResource soccerpitch = controller.getSoccerPitchResource();
		sendResource(soccerpitch);
		XMIResource update = receiveResource();
		return (Update) update.getContents().get(0);
	}
}
