import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Header } from '../../../../shared/components/header/header';

@Component({
  selector: 'app-home-page',
  imports: [Header, TranslateModule],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss'
})
export class HomePage {

}
