/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bdn2srt;

/**
 *
 * @author Cwlin
 */
public class CliBdnToSrtListener implements BdnToSrtListener {

	@Override
	public void changeStatus(String status) {
		System.out.println(status);
	}
	
}
