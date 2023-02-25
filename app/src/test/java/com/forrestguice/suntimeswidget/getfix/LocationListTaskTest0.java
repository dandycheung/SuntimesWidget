/**
    Copyright (C) 2022 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.getfix;

import android.database.MatrixCursor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LocationListTaskTest0
{
    @Test
    public void test_LocationListResult_new()
    {
        MatrixCursor cursor = new MatrixCursor(new String[] {"test1"});
        LocationListTask.LocationListTaskResult result0 = new LocationListTask.LocationListTaskResult(cursor, 1);
        assertEquals(cursor, result0.getCursor());
        assertEquals(1, result0.getIndex());
    }
}
