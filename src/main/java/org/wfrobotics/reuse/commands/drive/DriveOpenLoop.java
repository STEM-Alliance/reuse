package org.wfrobotics.reuse.commands.drive;

import org.wfrobotics.reuse.subsystems.drive.TankSubsystem;
import org.wfrobotics.reuse.EnhancedRobot;
import org.wfrobotics.reuse.config.EnhancedIO;

import edu.wpi.first.wpilibj2.command.CommandBase;

/** Bang bang control. Not a command you usually want outside rare, special situations */
public class DriveOpenLoop extends CommandBase
{
    private final TankSubsystem drive;
    private final EnhancedIO io = EnhancedRobot.getIO();
    private final double power;

    public DriveOpenLoop(double percentVoltage)
    {
        drive = TankSubsystem.getInstance();
        requires(drive);
        power = percentVoltage;
    }

    protected void initialize()
    {
        drive.setBrake(true);
    }

    protected void execute()
    {
        drive.driveOpenLoop(power, power);
    }

    protected boolean isFinished()
    {
        return io.isDriveOverrideRequested();
    }
}
