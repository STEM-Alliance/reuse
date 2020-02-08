package org.wfrobotics.reuse.commands.config;

import org.wfrobotics.reuse.subsystems.drive.TankSubsystem;

import edu.wpi.first.wpilibj2.command.InstantCommand;

public class GyroZero extends InstantCommand
{
    protected void initialize()
    {
        TankSubsystem.getInstance().setGyro(0.0);
    }
}
