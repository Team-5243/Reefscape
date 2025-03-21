// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.HttpCamera;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.util.LimelightHelpers;

public class CameraSubsystem extends SubsystemBase {
    
    private static CameraSubsystem instance;
    public UsbCamera lifecam;
    public HttpCamera limelight1;
    public HttpCamera limelight2;
    public AprilTagFieldLayout fieldLayout;

    /**
     * Creates a new CameraSubsystem
     */
    public CameraSubsystem() {
        instance = this;

        limelight1 = new HttpCamera(Constants.FRONT_LIMELIGHT, "http://10.52.43.11:5800");
        limelight2 = new HttpCamera(Constants.BACK_LIMELIGHT, "http://10.52.43.12:5800");

        fieldLayout = AprilTagFieldLayout.loadField(AprilTagFields.k2025ReefscapeAndyMark);

        CameraServer.startAutomaticCapture(0);
        CameraServer.startAutomaticCapture(limelight2);
        CameraServer.startAutomaticCapture(limelight2);
        LimelightHelpers.setPipelineIndex(Constants.FRONT_LIMELIGHT, Constants.ODOMETRY_PIPIELINE);
        
    }

    public static CameraSubsystem getInstance() {
        return instance;
    }

    /**
     * Example command factory method.
     *
     * @return a command
     */
    public Command exampleMethodCommand() {
        // Inline construction of command goes here.
        // Subsystem::RunOnce implicitly requires `this` subsystem.
        return runOnce(
                () -> {
                    /* one-time action goes here */
                });
    }

    /**
     * An example method querying a boolean state of the subsystem (for example, a
     * digital sensor).
     *
     * @return value of some boolean subsystem state, such as a digital sensor.
     */
    public boolean exampleCondition() {
        // Query some boolean state, such as a digital sensor.
        return false;
    }

    @Override
    public void periodic() {
        // This method will be called once per scheduler run
        
    }

    @Override
    public void simulationPeriodic() {
        // This method will be called once per scheduler run during simulation
    }
}
