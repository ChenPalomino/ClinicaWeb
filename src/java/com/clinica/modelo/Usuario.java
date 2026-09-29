/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.clinica.modelo;

/**
 *
 * @author ASUS
 */
public class Usuario {

    private int idUsuario;
    private String dni;
    private String nombres;
    private String celular;
    private String correo;
    private String passwordHash;
    private int idRol;

    // Constructor vacío (obligatorio para frameworks y POJOs)
    public Usuario() {
    }

    // Constructor con parámetros (sin ID porque es Autoincremental en BD)
    public Usuario(String dni, String nombres, String celular, String correo, String passwordHash, int idRol) {
        this.dni = dni;
        this.nombres = nombres;
        this.celular = celular;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.idRol = idRol;
    }

    // Getters y Setters
    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

}
