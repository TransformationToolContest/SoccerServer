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

public interface ISoccerPitchVisualizer {

	public void initialize(SoccerPitchController controller);

	public void visualize();

	public void showResult();

}
