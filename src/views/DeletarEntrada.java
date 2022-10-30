package views;

import java.util.Scanner;

import controllers.EntradaController;
import models.Entrada;

public class DeletarEntrada {

	private static Scanner print = new Scanner(System.in);
	private static Entrada entrada;

	public static void renderizar(){
		
		entrada = new Entrada();
		        
		System.out.println("\nDigite o ID da entrada que deseja deletar");
		int id = print.nextInt();
		entrada = EntradaController.buscarPorId(id);
		if (entrada != null){
			if(EntradaController.deletar(entrada)){
				System.out.println("\nRegistro de entrada excluído com sucesso");
			}
		} else{
			System.out.println("\nUm registro de entrada com este ID não existe");
		}
	}
}
