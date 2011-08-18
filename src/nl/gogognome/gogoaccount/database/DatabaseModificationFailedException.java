/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.database;

/**
 * This exception is thrown when database modification failed.
 */
public class DatabaseModificationFailedException extends Exception {

    public DatabaseModificationFailedException(String message) {
        super(message);
    }

    public DatabaseModificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
