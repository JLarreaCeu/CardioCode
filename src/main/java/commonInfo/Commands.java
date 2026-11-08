package commonInfo;

public enum Commands {
    LOGIN, REGISTER_PATIENT, REGISTER_DOCTOR, GET_DOCTORS,  //aun no login
    SEND_SYMPTOMS, START_RECORDING, SIGNAL_DATA, STOP_RECORDING, //paciente
    GET_PATIENTS, GET_PATIENT_HISTORY, GET_RECORDING_DATA, ADD_NOTE, UPDATE_PATIENT, //medico
    LOGOUT, //ambos
    OK, ERROR //respuestas del servidor

}
