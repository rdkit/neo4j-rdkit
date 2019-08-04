package org.rdkit.fingerprint;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

class Utils {

  private Utils() {

  }

  @SuppressWarnings("rawtypes")
  static boolean equals(final Object o1, final Object o2) {
    boolean bResult = false;

    if (o1 == o2) {
      bResult = true;
    } else if (o1 != null && o1.getClass().isArray() &&
        o2 != null && o2.getClass().isArray() &&
        o1.getClass().getComponentType().equals(o2.getClass().getComponentType()) &&
        Array.getLength(o1) == Array.getLength(o2)) {
      final int iLength = Array.getLength(o1);

      // Positive presumption
      bResult = true;

      for (int i = 0; i < iLength; i++) {
        if ((bResult &= equals(Array.get(o1, i), Array.get(o2, i))) == false) {
          break;
        }
      }
    } else if (o1 instanceof Collection && o2 instanceof Collection &&
        ((Collection) o1).size() == ((Collection) o2).size()) {
      final Iterator i1 = ((Collection) o1).iterator();
      final Iterator i2 = ((Collection) o2).iterator();

      // Positive presumption
      if (i1.hasNext() && i2.hasNext()) {
        bResult = true;

        while (i1.hasNext() && i2.hasNext()) {
          if ((bResult &= equals(i1.next(), i2.next())) == false) {
            break;
          }
        }
      }
    } else if (o1 != null && o2 != null) {
      bResult = o1.equals(o2);
    }

    return bResult;
  }

}
