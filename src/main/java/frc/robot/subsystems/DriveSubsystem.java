// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.util.Optional;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.ClosedLoopConfig;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.ClosedLoopConfig.FeedbackSensor;
import com.studica.frc.AHRS;
import com.studica.frc.AHRS.NavXComType;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.estimator.MecanumDrivePoseEstimator;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.MecanumDriveKinematics;
import edu.wpi.first.math.kinematics.MecanumDriveWheelPositions;
import edu.wpi.first.math.kinematics.MecanumDriveWheelSpeeds;
import edu.wpi.first.units.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotState;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.drive.MecanumDrive;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;

import frc.robot.Constants;
import frc.robot.util.LimelightHelpers;
import frc.robot.util.Utilities;

public class DriveSubsystem extends SubsystemBase {

	private static DriveSubsystem instance;

	public SparkMax fl;
	public SparkMax fr;
	public SparkMax bl;
	public SparkMax br;

	public RelativeEncoder flEncoder;
	public RelativeEncoder frEncoder;
	public RelativeEncoder blEncoder;
	public RelativeEncoder brEncoder;

	public SimpleMotorFeedforward flFeedForward;
	public SimpleMotorFeedforward frFeedForward;
	public SimpleMotorFeedforward blFeedForward;
	public SimpleMotorFeedforward brFeedForward;

	public SparkClosedLoopController flPID;
	public SparkClosedLoopController frPID;
	public SparkClosedLoopController blPID;
	public SparkClosedLoopController brPID;

	public MecanumDrive drive;

	public SlewRateLimiter limiter;

	public SysIdRoutine sysId;

	public AHRS gyro;

	public MecanumDriveKinematics kinematics;

	public ChassisSpeeds chassisSpeeds;

	public MecanumDrivePoseEstimator poseEstimator;

	/**
	 * Creates a new DriveSubsystem
	 */
	public DriveSubsystem() {
		this.fl = new SparkMax(Constants.FL, MotorType.kBrushless);
		this.fr = new SparkMax(Constants.FR, MotorType.kBrushless);
		this.bl = new SparkMax(Constants.BL, MotorType.kBrushless);
		this.br = new SparkMax(Constants.BR, MotorType.kBrushless);

		this.flEncoder = fl.getAlternateEncoder();
		this.frEncoder = fr.getAlternateEncoder();
		this.blEncoder = bl.getAlternateEncoder();
		this.brEncoder = br.getAlternateEncoder();

		this.fl.configure(
				new SparkMaxConfig().disableFollowerMode().inverted(false)
						.apply(new ClosedLoopConfig().pid(Constants.FL_kP, Constants.FL_kI, Constants.FL_kD)
								.feedbackSensor(FeedbackSensor.kAlternateOrExternalEncoder)),
				ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
		this.fr.configure(
				new SparkMaxConfig().disableFollowerMode().inverted(true)
						.apply(new ClosedLoopConfig().pid(Constants.FL_kP, Constants.FL_kI, Constants.FL_kD)
								.feedbackSensor(FeedbackSensor.kAlternateOrExternalEncoder)),
				ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
		this.bl.configure(
				new SparkMaxConfig().disableFollowerMode().inverted(false)
						.apply(new ClosedLoopConfig().pid(Constants.FL_kP, Constants.FL_kI, Constants.FL_kD)
								.feedbackSensor(FeedbackSensor.kAlternateOrExternalEncoder)),
				ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
		this.br.configure(
				new SparkMaxConfig().disableFollowerMode().inverted(true)
						.apply(new ClosedLoopConfig().pid(Constants.FL_kP, Constants.FL_kI, Constants.FL_kD)
								.feedbackSensor(FeedbackSensor.kAlternateOrExternalEncoder)),
				ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

		this.flFeedForward = new SimpleMotorFeedforward(Constants.FL_kS, Constants.FL_kV, Constants.FL_kA);
		this.frFeedForward = new SimpleMotorFeedforward(Constants.FR_kS, Constants.FR_kV, Constants.FR_kA);
		this.blFeedForward = new SimpleMotorFeedforward(Constants.BL_kS, Constants.BL_kV, Constants.BL_kA);
		this.brFeedForward = new SimpleMotorFeedforward(Constants.BR_kS, Constants.BR_kV, Constants.BR_kA);

		this.flPID = fl.getClosedLoopController();
		this.frPID = fr.getClosedLoopController();
		this.blPID = bl.getClosedLoopController();
		this.brPID = br.getClosedLoopController();

		this.drive = new MecanumDrive(fl, bl, fr, br);

		this.sysId = new SysIdRoutine(new SysIdRoutine.Config(), new SysIdRoutine.Mechanism(
				voltage -> {
					fl.setVoltage(voltage.magnitude());
					fr.setVoltage(voltage.magnitude());
					bl.setVoltage(voltage.magnitude());
					br.setVoltage(voltage.magnitude());
					System.out.println("setting: " + voltage.magnitude() + "; getteing " + fl.getBusVoltage());
				},
				log -> {
					log.motor("drive-front-right")
							.voltage(Units.Volts.of(fr.getBusVoltage()))
							.linearPosition(Utilities.rotationsToDistance(flEncoder.getPosition()))
							.linearVelocity(Units.MetersPerSecond.of(frEncoder.getVelocity() * 60 * Math.PI
									* Constants.WHEEL_DIAMETER.in(Units.Meters)));

					log.motor("drive-front-left")
							.voltage(Units.Volts.of(fl.getBusVoltage()))
							.linearPosition(Utilities.rotationsToDistance(flEncoder.getPosition()))
							.linearVelocity(Units.MetersPerSecond.of(flEncoder.getVelocity() * 60 * Math.PI
									* Constants.WHEEL_DIAMETER.in(Units.Meters)));

					log.motor("drive-back-left")
							.voltage(Units.Volts.of(bl.getBusVoltage()))
							.linearPosition(Utilities.rotationsToDistance(blEncoder.getPosition()))
							.linearVelocity(Units.MetersPerSecond.of(blEncoder.getVelocity() * 60 * Math.PI
									* Constants.WHEEL_DIAMETER.in(Units.Meters)));

					log.motor("drive-back-right")
							.voltage(Units.Volts.of(br.getBusVoltage()))
							.linearPosition(Utilities.rotationsToDistance(brEncoder.getPosition()))
							.linearVelocity(Units.MetersPerSecond.of(brEncoder.getVelocity() * 60 * Math.PI
									* Constants.WHEEL_DIAMETER.in(Units.Meters)));
				},
				this));

		this.gyro = new AHRS(NavXComType.kUSB1);

		this.gyro.reset();

		// Make it so getAngle is zero when facing red aliiance station
		// Needed for field coordinates, which are based on blue alliance.
		if (DriverStation.getAlliance().orElse(Alliance.Red) == Alliance.Blue)
			gyro.setAngleAdjustment(180);

		limiter = new SlewRateLimiter(.5);

		/*
		 * TODO: - Get wheel positions on robot.
		 * - Get initial pose
		 * - Get max drive speed
		 */
		this.kinematics = new MecanumDriveKinematics(new Translation2d(0.259, 0.283), new Translation2d(0.259, -0.283),
				new Translation2d(-0.259, 0.283), new Translation2d(-0.259, -0.283));

		this.poseEstimator = new MecanumDrivePoseEstimator(kinematics, this.gyro.getRotation2d(),
				new MecanumDriveWheelPositions(), null);

		RobotConfig config;

		try {
			config = RobotConfig.fromGUISettings();
		} catch (Exception e) {
			// TODO: handle exception
			config = null;
			e.printStackTrace();
		}

		AutoBuilder.configure(
				this::getPose,
				this::setPose,
				this::getChassisSpeeds,
				this::driveRobotSpeed,
				new PPHolonomicDriveController(
						new PIDConstants(1, 0, 0),
						new PIDConstants(1.2, 0, 0)),
				config,
				() -> {
					Optional<Alliance> alliance = DriverStation.getAlliance();
					if (alliance.isPresent()) {
						return alliance.get() == DriverStation.Alliance.Red;
					}
					return false;
				},
				this);

		instance = this;
	}

	/**
	 * Uses input to drive the mechanum chassis (robot centric)
	 * 
	 * @param xSpeed The robot's speed along the X axis [-1.0..1.0]. Forward is
	 *               positive.
	 * @param ySpeed The robot's speed along the Y axis [-1.0..1.0]. Left is
	 *               positive.
	 * @param zSpeed The robot's rotation rate around the Z axis [-1.0..1.0].
	 *               Counterclockwise is positive.
	 */
	public void mechDrive(double xSpeed, double ySpeed, double zSpeed) {
		drive.driveCartesian(xSpeed, ySpeed, zSpeed);
	}

	/**
	 * Applies deadzones and exponential scaling to input and uses them to drive the
	 * robot (robot centric)
	 * 
	 * @param xSpeed The robot's speed along the X axis [-1.0..1.0]. Forward is
	 *               positive.
	 * @param ySpeed The robot's speed along the Y axis [-1.0..1.0]. Left is
	 *               positive.
	 * @param zSpeed The robot's rotation rate around the Z axis [-1.0..1.0].
	 *               Counterclockwise is positive.
	 */
	public void DSMechDrive(double xSpeed, double ySpeed, double zSpeed) {
		// Apply deadzone
		double stickDeadzone = 0.1;
		double squaredMag = xSpeed * xSpeed + ySpeed * ySpeed;
		if (squaredMag < stickDeadzone * stickDeadzone)
			xSpeed = ySpeed = 0;

		if (Math.abs(zSpeed) < stickDeadzone)
			zSpeed = 0;

		if (squaredMag > 1)
			squaredMag = 1;

		// Apply exponential rates
		// if (xSpeed != 0 || ySpeed != 0) {
		// squaredMag = Math.sqrt(squaredMag);
		// xSpeed *= squaredMag;
		// ySpeed *= squaredMag;
		// }
		// if (zSpeed != 0) zSpeed = zSpeed * Math.abs(zSpeed);

		drive.driveCartesian(xSpeed, ySpeed, zSpeed);
	}

	/**
	 * Uses joysticks to drive the mechanum chassis while using a slew rate limiter
	 * (robot centric)
	 */
	public void mechDriveLimiter() {
		mechDrive(limiter.calculate(-Constants.primaryStick.getY()), limiter.calculate(Constants.primaryStick.getX()),
				limiter.calculate(Constants.secondaryStick.getX()));
	}

	/**
	 * Uses joysticks to drive the mechanum chassis while using deadzones (robot
	 * centric)
	 */
	public void mechDrive() {
		System.out.println(Constants.primaryStick.getPOV());
		DSMechDrive(-Constants.primaryStick.getY(), Constants.primaryStick.getX(), Constants.secondaryStick.getX());
	}

	/**
	 * Uses input to drive the mechanum chassis (field centric)
	 * 
	 * @param xSpeed The robot's speed along the X axis [-1.0..1.0]. Forward is
	 *               positive.
	 * @param ySpeed The robot's speed along the Y axis [-1.0..1.0]. Left is
	 *               positive.
	 * @param zSpeed The robot's rotation rate around the Z axis [-1.0..1.0].
	 *               Counterclockwise is positive.
	 */
	public void fieldMechDrive(double xSpeed, double ySpeed, double zSpeed) {
		drive.driveCartesian(xSpeed, ySpeed, zSpeed, gyro.getRotation2d());
	}

	/**
	 * Uses joysticks to drive the mechanum chassis (field centric)
	 */
	public void fieldMechDrive() {
		fieldMechDrive(-Constants.primaryStick.getY(), Constants.primaryStick.getX(), Constants.secondaryStick.getX());
	}

	public void updatePoseEstimate() {
		poseEstimator.update(gyro.getRotation2d(),
				new MecanumDriveWheelPositions(Utilities.rotationsToDistance(flEncoder.getPosition()),
						Utilities.rotationsToDistance(frEncoder.getPosition()),
						Utilities.rotationsToDistance(blEncoder.getPosition()),
						Utilities.rotationsToDistance(brEncoder.getPosition())));

		// Modified from
		// https://docs.limelightvision.io/docs/docs-limelight/tutorials/tutorial-swerve-pose-estimation

		boolean useMegaTag2 = true; // set to false to use MegaTag1
		boolean doRejectUpdate = false;

		// For Front limelight
		if (useMegaTag2 == false) {
			LimelightHelpers.PoseEstimate mt1 = LimelightHelpers.getBotPoseEstimate_wpiBlue(Constants.FRONT_LIMELIGHT);

			if (mt1.tagCount == 1 && mt1.rawFiducials.length == 1) {
				if (mt1.rawFiducials[0].ambiguity > .7) {
					doRejectUpdate = true;
				}
				if (mt1.rawFiducials[0].distToCamera > 3) {
					doRejectUpdate = true;
				}
			}
			if (mt1.tagCount == 0) {
				doRejectUpdate = true;
			}

			if (!doRejectUpdate) {
				poseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(.5, .5, 9999999));
				poseEstimator.addVisionMeasurement(
						mt1.pose,
						mt1.timestampSeconds);
			}

		} else if (useMegaTag2 == true) {
			LimelightHelpers.SetRobotOrientation(Constants.FRONT_LIMELIGHT,
					poseEstimator.getEstimatedPosition().getRotation().getDegrees(), 0, 0, 0, 0, 0);
			LimelightHelpers.PoseEstimate mt2 = LimelightHelpers
					.getBotPoseEstimate_wpiBlue_MegaTag2(Constants.FRONT_LIMELIGHT);
			if (Math.abs(gyro.getRate()) > 720) // if our angular velocity is greater than 720 degrees per second,
												// ignore vision updates
			{
				doRejectUpdate = true;
			}
			if (mt2.tagCount == 0) {
				doRejectUpdate = true;
			}
			if (!doRejectUpdate) {
				poseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(.7, .7, 9999999));
				poseEstimator.addVisionMeasurement(
						mt2.pose,
						mt2.timestampSeconds);
			}
		}

		// For back limelight
		if (useMegaTag2 == false) {
			LimelightHelpers.PoseEstimate mt1 = LimelightHelpers.getBotPoseEstimate_wpiBlue(Constants.BACK_LIMELIGHT);

			if (mt1.tagCount == 1 && mt1.rawFiducials.length == 1) {
				if (mt1.rawFiducials[0].ambiguity > .7) {
					doRejectUpdate = true;
				}
				if (mt1.rawFiducials[0].distToCamera > 3) {
					doRejectUpdate = true;
				}
			}
			if (mt1.tagCount == 0) {
				doRejectUpdate = true;
			}

			if (!doRejectUpdate) {
				poseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(.5, .5, 9999999));
				poseEstimator.addVisionMeasurement(
						mt1.pose,
						mt1.timestampSeconds);
			}

		} else if (useMegaTag2 == true) {
			LimelightHelpers.SetRobotOrientation(Constants.BACK_LIMELIGHT,
					poseEstimator.getEstimatedPosition().getRotation().getDegrees(), 0, 0, 0, 0, 0);
			LimelightHelpers.PoseEstimate mt2 = LimelightHelpers
					.getBotPoseEstimate_wpiBlue_MegaTag2(Constants.BACK_LIMELIGHT);
			if (Math.abs(gyro.getRate()) > 720) // if our angular velocity is greater than 720 degrees per second,
												// ignore vision updates
			{
				doRejectUpdate = true;
			}
			if (mt2.tagCount == 0) {
				doRejectUpdate = true;
			}
			if (!doRejectUpdate) {
				poseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(.7, .7, 9999999));
				poseEstimator.addVisionMeasurement(
						mt2.pose,
						mt2.timestampSeconds);
			}
		}
	}

	public Pose2d getPose() {
		return poseEstimator.getEstimatedPosition();
	}

	public void setPose(Pose2d pose) {
		poseEstimator.resetPose(pose);
	}

	public ChassisSpeeds getChassisSpeeds() {
		return kinematics.toChassisSpeeds(
				new MecanumDriveWheelSpeeds(
						Units.RPM.of(flEncoder.getVelocity()).in(Units.RadiansPerSecond)
								* Constants.WHEEL_DIAMETER.in(Units.Meters) / 2,
						Units.RPM.of(frEncoder.getVelocity()).in(Units.RadiansPerSecond)
								* Constants.WHEEL_DIAMETER.in(Units.Meters) / 2,
						Units.RPM.of(blEncoder.getVelocity()).in(Units.RadiansPerSecond)
								* Constants.WHEEL_DIAMETER.in(Units.Meters) / 2,
						Units.RPM.of(brEncoder.getVelocity()).in(Units.RadiansPerSecond)
								* Constants.WHEEL_DIAMETER.in(Units.Meters) / 2));
	}

	public void driveRobotSpeed(ChassisSpeeds speeds) {
		MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

		flPID.setReference(Units.RadiansPerSecond
				.of(wheelSpeeds.frontLeftMetersPerSecond / (Constants.WHEEL_DIAMETER.in(Units.Meters) / 2))
				.in(Units.RPM),
				ControlType.kVelocity);

		frPID.setReference(Units.RadiansPerSecond
				.of(wheelSpeeds.frontRightMetersPerSecond / (Constants.WHEEL_DIAMETER.in(Units.Meters) / 2))
				.in(Units.RPM),
				ControlType.kVelocity);

		frPID.setReference(Units.RadiansPerSecond
				.of(wheelSpeeds.rearRightMetersPerSecond / (Constants.WHEEL_DIAMETER.in(Units.Meters) / 2))
				.in(Units.RPM),
				ControlType.kVelocity);

		frPID.setReference(Units.RadiansPerSecond
				.of(wheelSpeeds.rearLeftMetersPerSecond / (Constants.WHEEL_DIAMETER.in(Units.Meters) / 2))
				.in(Units.RPM),
				ControlType.kVelocity);
	}

	public static DriveSubsystem getInstance() {
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
		if (RobotState.isEnabled())
			updatePoseEstimate();
		System.out.println("x: " + flEncoder.getVelocity());
	}

	@Override
	public void simulationPeriodic() {
		// This method will be called once per scheduler run during simulation
	}
}
