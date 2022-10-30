package controllers;

import java.util.ArrayList;

import models.Funcionario;
import models.Produto;

public class FuncionarioController {

	private static ArrayList<Funcionario> funcionarios = new ArrayList<>();

	//Cadastrar
	public static void cadastrar(Funcionario funcionario){
		funcionarios.add(funcionario);
	}

	//Listar todos os funcionarios
	public static ArrayList<Funcionario> listar(){
		return funcionarios;
	}

	//Buscar por matricula
	public static Funcionario buscarPorMatricula(String matricula){
		for(Funcionario funcionarioCadastrado: funcionarios){
			if (funcionarioCadastrado.getMatricula().equals(matricula)){
				return funcionarioCadastrado;
			}
		}
		return null;    
	}
	
	


}
