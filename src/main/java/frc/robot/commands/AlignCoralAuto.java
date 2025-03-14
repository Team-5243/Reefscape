// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.units.Units;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.subsystems.CameraSubsystem;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.util.LimelightHelpers;

/** An example command that uses an example subsystem. */
public class AlignCoralAuto extends Command {
    @SuppressWarnings({ "PMD.UnusedPrivateField", "PMD.SingularField" })
    private final DriveSubsystem m_driveSubsystem;
    private final CameraSubsystem m_cameraSubsystem;
    private int pipeline;
    private boolean turning;
    private Timer time;
    private double tolerance = 2;

    /**
     * Creates a new Wait command.
     * <p>
     * Waits for the specified number of seconds.
     * <p>
     * Disables WPILib motor safty during waiting period.
     *
     * @param time How long to wait (in seconds)
     */
    public AlignCoralAuto(DriveSubsystem driveSubsystem, CameraSubsystem cameraSubsystem) {
        this(driveSubsystem, cameraSubsystem, 2);
    }

    public AlignCoralAuto(DriveSubsystem driveSubsystem, CameraSubsystem cameraSubsystem, int pipeline) {
        this(driveSubsystem, cameraSubsystem, pipeline, false);
    }

    public AlignCoralAuto(DriveSubsystem driveSubsystem, CameraSubsystem cameraSubsystem, int pipeline, boolean turn) {
        this.m_driveSubsystem = driveSubsystem;
        this.m_cameraSubsystem = cameraSubsystem;
        this.pipeline = pipeline;
        this.turning = turn;
        this.time = new Timer();
        // Use addRequirements() here to declare subsystem dependencies.
        addRequirements(m_driveSubsystem, m_cameraSubsystem);
    }

    // Called when the command is initially scheduled.
    @Override
    public void initialize() {
        LimelightHelpers.setPipelineIndex(Constants.FRONT_LIMELIGHT, pipeline);
        time.restart();
    }

    // Called every time the scheduler runs while the command is scheduled.
    @Override
    public void execute() {
        double x = LimelightHelpers.getTX(Constants.FRONT_LIMELIGHT);
        if (x < -tolerance || x > tolerance) {
            // formatting diff

            double strafe = (x) / (150);
            strafe = strafe > 1 ? 1 : strafe;
            strafe = strafe < -1 ? -1 : strafe;
            strafe = Math.abs(strafe) < .075 ? Math.signum(strafe) * .075 : strafe;


            double turn = 0;

            m_driveSubsystem.mechDrive(0, strafe, turn);

        }
    }

    // Called once the command ends or is interrupted.
    @Override
    public void end(boolean interrupted) {
        LimelightHelpers.setPipelineIndex(Constants.FRONT_LIMELIGHT, 0);
        m_driveSubsystem.mechDrive(0, 0, 0);
    }

    // Returns true when the command should end.
    @Override
    public boolean isFinished() {
        double x = LimelightHelpers.getTX(Constants.FRONT_LIMELIGHT);
        return !(x < -tolerance || x > tolerance) && time.hasElapsed(3);
    }
}
