/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.collision.geometry;

import me.moros.math.FastMath;
import me.moros.math.Vector3d;

final class ColliderUtil {
  private ColliderUtil() {
  }

  static boolean intersects(Collider first, Collider second) {
    if (first.equals(AABB.dummy()) || second.equals(AABB.dummy())) {
      return false;
    } else if (first instanceof Sphere sphere1) {
      if (second instanceof Sphere sphere2) {
        return sphere1._intersects(sphere2);
      } else if (second instanceof AABB aabb) {
        return _intersects(aabb, sphere1);
      } else if (second instanceof OBB obb) {
        return _intersects(obb, sphere1);
      } else if (second instanceof Disk disk) {
        return disk.sphere()._intersects(sphere1) && _intersects(disk.obb(), sphere1);
      } else if (second instanceof Ray ray) {
        return _intersects(ray, sphere1);
      }
    } else if (first instanceof AABB aabb1) {
      if (second instanceof AABB aabb2) {
        return aabb1._intersects(aabb2);
      } else if (second instanceof OBB obb) {
        return obb._intersects(new OBB(aabb1));
      } else if (second instanceof Sphere sphere) {
        return _intersects(aabb1, sphere);
      } else if (second instanceof Disk disk) {
        return _intersects(aabb1, disk.sphere()) && disk.obb()._intersects(new OBB(aabb1));
      } else if (second instanceof Ray ray) {
        return _intersects(ray, aabb1);
      }
    } else if (first instanceof OBB obb1) {
      if (second instanceof OBB obb2) {
        return obb1._intersects(obb2);
      } else if (second instanceof AABB aabb) {
        return obb1._intersects(new OBB(aabb));
      } else if (second instanceof Sphere sphere) {
        return _intersects(obb1, sphere);
      } else if (second instanceof Disk disk) {
        return _intersects(obb1, disk.sphere()) && obb1._intersects(disk.obb());
      } else if (second instanceof Ray ray) {
        return _intersects(obb1, ray);
      }
    } else if (first instanceof Disk disk1) {
      if (second instanceof Disk disk2) {
        return disk1.sphere()._intersects(disk2.sphere()) && disk1.obb()._intersects(disk2.obb());
      } else if (second instanceof AABB aabb) {
        return _intersects(aabb, disk1.sphere()) && disk1.obb()._intersects(new OBB(aabb));
      } else if (second instanceof OBB obb) {
        return _intersects(obb, disk1.sphere()) && obb._intersects(disk1.obb());
      } else if (second instanceof Sphere sphere) {
        return disk1.sphere()._intersects(sphere) && _intersects(disk1.obb(), sphere);
      } else if (second instanceof Ray ray) {
        return _intersects(ray, disk1.sphere()) && _intersects(disk1.obb(), ray);
      }
    } else if (first instanceof Ray ray1) {
      if (second instanceof Ray ray2) {
        return ray1._intersects(ray2);
      } else if (second instanceof AABB aabb) {
        return _intersects(ray1, aabb);
      } else if (second instanceof OBB obb) {
        return _intersects(obb, ray1);
      } else if (second instanceof Sphere sphere) {
        return _intersects(ray1, sphere);
      } else if (second instanceof Disk disk) {
        return _intersects(ray1, disk.sphere()) && _intersects(disk.obb(), ray1);
      }
    }
    return false;
  }

  private static boolean _intersects(AABB aabb, Sphere sphere) {
    Vector3d min = aabb.min;
    Vector3d max = aabb.max;
    // Get the point closest to sphere center on the aabb.
    double x = FastMath.clamp(sphere.center.x(), min.x(), max.x());
    double y = FastMath.clamp(sphere.center.y(), min.y(), max.y());
    double z = FastMath.clamp(sphere.center.z(), min.z(), max.z());
    // Check if that point is inside the sphere.
    return sphere.contains(Vector3d.of(x, y, z));
  }

  private static boolean _intersects(OBB obb, Sphere sphere) {
    Vector3d v = sphere.center.subtract(obb.closestPosition(sphere.center));
    return v.dot(v) <= sphere.radius * sphere.radius;
  }

  private static boolean _intersects(OBB obb, Ray ray) {
    Ray localRay = new Ray(obb.localSpace(ray.origin), obb.localSpace(ray.direction));
    AABB localAABB = new AABB(obb.e.negate(), obb.e).at(obb.position());
    return _intersects(localRay, localAABB);
  }

  private static boolean _intersects(Ray ray, Sphere sphere) {
    Vector3d m = ray.origin.subtract(sphere.center);
    double b = m.dot(ray.direction);
    return b * b - (m.dot(m) - sphere.radius * sphere.radius) >= 0;
  }

  private static boolean _intersects(Ray ray, AABB aabb) {
    Vector3d t0 = aabb.min.subtract(ray.origin).multiply(ray.invDir);
    Vector3d t1 = aabb.max.subtract(ray.origin).multiply(ray.invDir);
    return t0.min(t1).maxComponent() <= t0.max(t1).minComponent();
  }
}
