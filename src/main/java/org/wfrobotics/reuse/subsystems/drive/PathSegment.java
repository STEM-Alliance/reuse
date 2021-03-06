package org.wfrobotics.reuse.subsystems.drive;

import java.util.Optional;

import org.wfrobotics.reuse.math.geometry.Rotation2d;
import org.wfrobotics.reuse.math.geometry.Translation2d;
import org.wfrobotics.reuse.math.motion.MotionProfile;
import org.wfrobotics.reuse.math.motion.MotionProfileConstraints;
import org.wfrobotics.reuse.math.motion.MotionProfileGenerator;
import org.wfrobotics.reuse.math.motion.MotionProfileGoal;
import org.wfrobotics.reuse.math.motion.MotionState;
import org.wfrobotics.reuse.subsystems.drive.TankMaths;

/**
 * A path in absolute coordiates
 *
 * <p>Class representing a segment of the robot's autonomous path.
 * @author Code: Team 254<br>Documentation: STEM Alliance of Fargo Moorhead
 */
public class PathSegment {
    private static final double kPathFollowingMaxAccel = TankMaths.kVelocityMaxInchesPerSecond;

    private Translation2d start;
    private Translation2d end;
    private Translation2d center;
    private Translation2d deltaStart;
    private Translation2d deltaEnd;
    private double maxSpeed;
    private boolean isLine;
    private MotionProfile speedController;
    private boolean extrapolateLookahead;

    /**
     * Constructor for a linear segment
     *
     * @param x1
     *            start x
     * @param y1
     *            start y
     * @param x2
     *            end x
     * @param y2
     *            end y
     * @param maxSpeed
     *            maximum speed allowed on the segment
     */
    public PathSegment(double x1, double y1, double x2, double y2, double maxSpeed, MotionState startState, double endSpeed) {
        start = new Translation2d(x1, y1);
        end = new Translation2d(x2, y2);

        deltaStart = new Translation2d(start, end);

        this.maxSpeed = maxSpeed;
        extrapolateLookahead = false;
        isLine = true;
        createMotionProfiler(startState, endSpeed);
    }

    /**
     * Constructor for an arc segment
     *
     * @param x1
     *            start x
     * @param y1
     *            start y
     * @param x2
     *            end x
     * @param y2
     *            end y
     * @param cx
     *            center x
     * @param cy
     *            center y
     * @param maxSpeed
     *            maximum speed allowed on the segment
     */
    public PathSegment(double x1, double y1, double x2, double y2, double cx, double cy, double maxSpeed, MotionState startState, double endSpeed) {
        start = new Translation2d(x1, y1);
        end = new Translation2d(x2, y2);
        center = new Translation2d(cx, cy);

        deltaStart = new Translation2d(center, start);
        deltaEnd = new Translation2d(center, end);

        this.maxSpeed = maxSpeed;
        extrapolateLookahead = false;
        isLine = false;
        createMotionProfiler(startState, endSpeed);
    }

    /**
     * @return max speed of the segment
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void createMotionProfiler(MotionState start_state, double end_speed) {
        MotionProfileConstraints motionConstraints = new MotionProfileConstraints(maxSpeed, kPathFollowingMaxAccel);
        MotionProfileGoal goal_state = new MotionProfileGoal(getLength(), end_speed);
        speedController = MotionProfileGenerator.generateProfile(motionConstraints, goal_state, start_state);
        // System.out.println(speedController);
    }

    /**
     * @return starting point of the segment
     */
    public Translation2d getStart() {
        return start;
    }

    /**
     * @return end point of the segment
     */
    public Translation2d getEnd() {
        return end;
    }

    /**
     * @return the total length of the segment
     */
    public double getLength() {
        if (isLine) {
            return deltaStart.norm();
        }
        return deltaStart.norm() * Translation2d.getAngle(deltaStart, deltaEnd).getRadians();
    }

    /**
     * Set whether or not to extrapolate the lookahead point. Should only be true for the last segment in the path
     *
     * @param val
     */
    public void extrapolateLookahead(boolean val) {
        extrapolateLookahead = val;
    }

    /**
     * Gets the point on the segment closest to the robot
     *
     * @param position
     *            the current position of the robot
     * @return the point on the segment closest to the robot
     */
    public Translation2d getClosestPoint(Translation2d position) {
        if (isLine) {
            Translation2d delta = new Translation2d(start, end);
            double u = ((position.x() - start.x()) * delta.x() + (position.y() - start.y()) * delta.y())
                                            / (delta.x() * delta.x() + delta.y() * delta.y());
            if (u >= 0 && u <= 1)
                return new Translation2d(start.x() + u * delta.x(), start.y() + u * delta.y());
            return (u < 0) ? start : end;
        }
        Translation2d deltaPosition = new Translation2d(center, position);
        deltaPosition = deltaPosition.scale(deltaStart.norm() / deltaPosition.norm());
        if (Translation2d.cross(deltaPosition, deltaStart) * Translation2d.cross(deltaPosition, deltaEnd) < 0) {
            return center.translateBy(deltaPosition);
        }
        Translation2d startDist = new Translation2d(position, start);
        Translation2d endDist = new Translation2d(position, end);
        return (endDist.norm() < startDist.norm()) ? end : start;
    }

    /**
     * Calculates the point on the segment <code>dist</code> distance from the starting point along the segment.
     *
     * @param dist
     *            distance from the starting point
     * @return point on the segment <code>dist</code> distance from the starting point
     */
    public Translation2d getPointByDistance(double dist) {
        double length = getLength();
        if (!extrapolateLookahead && dist > length) {
            dist = length;
        }
        if (isLine) {
            return start.translateBy(deltaStart.scale(dist / length));
        }
        double deltaAngle = Translation2d.getAngle(deltaStart, deltaEnd).getRadians()
                                        * ((Translation2d.cross(deltaStart, deltaEnd) >= 0) ? 1 : -1);
        deltaAngle *= dist / length;
        Translation2d t = deltaStart.rotateBy(Rotation2d.fromRadians(deltaAngle));
        return center.translateBy(t);
    }

    /**
     * Gets the remaining distance left on the segment from point <code>point</code>
     *
     * @param point
     *            result of <code>getClosestPoint()</code>
     * @return distance remaining
     */
    public double getRemainingDistance(Translation2d position) {
        if (isLine) {
            return new Translation2d(end, position).norm();
        }
        Translation2d deltaPosition = new Translation2d(center, position);
        double angle = Translation2d.getAngle(deltaEnd, deltaPosition).getRadians();
        double totalAngle = Translation2d.getAngle(deltaStart, deltaEnd).getRadians();
        return angle / totalAngle * getLength();
    }

    private double getDistanceTravelled(Translation2d robotPosition) {
        Translation2d pathPosition = getClosestPoint(robotPosition);
        double remainingDist = getRemainingDistance(pathPosition);
        return getLength() - remainingDist;

    }

    public double getSpeedByDistance(double dist) {
        if (dist < speedController.startPos()) {
            dist = speedController.startPos();
        } else if (dist > speedController.endPos()) {
            dist = speedController.endPos();
        }
        Optional<MotionState> state = speedController.firstStateByPos(dist);
        if (state.isPresent()) {
            return state.get().vel();
        }
        System.out.println("Velocity does not exist at that position!");
        return 0.0;
    }

    public double getSpeedByClosestPoint(Translation2d robotPosition) {
        return getSpeedByDistance(getDistanceTravelled(robotPosition));
    }

    public MotionState getEndState() {
        return speedController.endState();
    }

    public MotionState getStartState() {
        return speedController.startState();
    }

    public String toString() {
        if (isLine) {
            return "(" + "start: " + start + ", end: " + end + ", speed: " + maxSpeed // + ", profile: " +
                                            // speedController
                                            + ")";
        }
        return "(" + "start: " + start + ", end: " + end + ", center: " + center + ", speed: " + maxSpeed
                                        + ")"; // + ", profile: " + speedController + ")";
    }
}
