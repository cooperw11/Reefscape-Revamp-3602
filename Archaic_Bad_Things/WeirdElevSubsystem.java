package frc.team3602.robot.subsystems.elevator;

import static edu.wpi.first.units.Units.RotationsPerSecond;
import static frc.team3602.robot.Constants.HardwareConstants.*;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.simulation.ElevatorSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**Weird subsystem that NO LONGER uses the weird utility type class that i didnt like */
//AKA its just a subsys that uses motion magic
public class WeirdElevSubsystem extends SubsystemBase{
    private final TalonFX leadMotor = new TalonFX(ELEV_LEAD_MOTOR_ID);
    private final TalonFX followerMotor = new TalonFX(ELEV_FOLLOW_MOTOR_ID);

    public final double startingHeight = 0.6;
    private double setpoint = startingHeight;
    
    private final MotionMagicVoltage controller = new MotionMagicVoltage(setpoint);

    public final ElevatorSim elevSim = new ElevatorSim(DCMotor.getKrakenX60(2), ELEV_GEARING, 8, 0.5, -0.01, Units.inchesToMeters(72), true, startingHeight);

    public WeirdElevSubsystem() {
        TalonFXConfiguration cfg = new TalonFXConfiguration();

        MotorOutputConfigs outputCfg = cfg.MotorOutput;
        outputCfg.NeutralMode = NeutralModeValue.Brake;
        outputCfg.Inverted = InvertedValue.CounterClockwise_Positive;

        CurrentLimitsConfigs limitCfg = cfg.CurrentLimits;
        limitCfg.StatorCurrentLimit = ELEV_CURRENT_LIMIT;

        FeedbackConfigs feedbackCfg = cfg.Feedback;
        feedbackCfg.SensorToMechanismRatio = ELEV_GEARING;

        MotionMagicConfigs controllerCfg = cfg.MotionMagic;
        controllerCfg.withMotionMagicCruiseVelocity(RotationsPerSecond.of(20)).withMotionMagicAcceleration(10)
        .withMotionMagicJerk(80);
        //TODO up with testing irl

        Slot0Configs slot0 = cfg.Slot0;

        if (Utils.isSimulation()) {
            slot0.kS = 0.0;
            slot0.kG = 0.05;//0.091;// PRE VOLTAGE MULTIPLICATION -> 0.69;//.7> && .67< 
            slot0.kA = 0.01;
            slot0.kV = 0.01;
            slot0.kP = 0.4;
            slot0.kI = 0.0;
            slot0.kD = 0.0;
        } else {
            slot0.kS = 0.0;
            slot0.kG = 1.0;
            slot0.kA = 0.2;
            slot0.kV = 0.1;
            slot0.kP = 0.0;
            slot0.kI = 0.0;
            slot0.kD = 0.0;
        }

        //elevator = new TalonElevator("Elevator", leadMotor, followerMotor, false, 0, cfg, elevSim);
    }

    public Command setHeight(double newHeight){
        return runOnce(() ->{
            setpoint = newHeight;
        });
    }

        /**method that returns the elevator rotor position (or meters in a simulation) */
        public double getEncoder() {
            if (Utils.isSimulation()) {
                return elevSim.getPositionMeters();
            } else {
                return leadMotor.getRotorPosition().getValueAsDouble();
            }
        }

    public boolean isNearGoal(){
        return MathUtil.isNear(setpoint, getEncoder(), 1.5);
    }

    @Override 
    public void simulationPeriodic(){
        elevSim.setInput(leadMotor.getMotorVoltage().getValueAsDouble() * 16);
        elevSim.update(0.001);    }

    @Override
    public void periodic(){
        SmartDashboard.putNumber("Elevator encoder", getEncoder());
        SmartDashboard.putNumber("Elevator setpoint", setpoint);

        SmartDashboard.putNumber("Elevator set voltage", leadMotor.getMotorVoltage().getValueAsDouble());
        SmartDashboard.putNumber("Elevator follower set voltage",
                followerMotor.getMotorVoltage().getValueAsDouble());

        SmartDashboard.putNumber("Elevator velocity", leadMotor.getVelocity().getValueAsDouble());
        leadMotor.setControl(controller.withPosition(setpoint - getEncoder()).withSlot(0));
    }
}
