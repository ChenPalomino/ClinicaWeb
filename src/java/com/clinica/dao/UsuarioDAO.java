package com.clinica.dao;

import com.clinica.modelo.Usuario;
import com.clinica.util.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    public boolean registrarUsuario(Usuario usuario) {
        String sql = "INSERT INTO tm_usuario (dni, nombres, celular, correo, password_hash, id_rol) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Connection con = ConexionBD.getInstancia().getConexion();
            if (con == null) {
                System.err.println("ERROR CRÍTICO: La conexión a PostgreSQL es NULL.");
                return false;
            }
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, usuario.getDni());
            ps.setString(2, usuario.getNombres());
            ps.setString(3, usuario.getCelular());
            ps.setString(4, usuario.getCorreo());
            ps.setString(5, usuario.getPasswordHash());
            ps.setInt(6, usuario.getIdRol());

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error SQL al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    public Usuario buscarPorDni(String dni) {
        String sql = "SELECT * FROM tm_usuario WHERE dni = ?";
        Usuario usuario = null;

        try {
            Connection con = ConexionBD.getInstancia().getConexion();
            if (con == null) {
                System.err.println("ERROR CRÍTICO: La conexión a PostgreSQL es NULL en el Login.");
                return null;
            }
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, dni);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setDni(rs.getString("dni"));
                usuario.setNombres(rs.getString("nombres"));
                usuario.setCelular(rs.getString("celular"));
                usuario.setCorreo(rs.getString("correo"));
                usuario.setPasswordHash(rs.getString("password_hash"));
                usuario.setIdRol(rs.getInt("id_rol"));
                System.out.println("DAO: Usuario encontrado -> " + usuario.getNombres());
            } else {
                System.out.println("DAO: No se encontró ningún registro con el DNI: " + dni);
            }
        } catch (SQLException e) {
            System.err.println("Error SQL al buscar usuario: " + e.getMessage());
        }
        return usuario;
    }
}
