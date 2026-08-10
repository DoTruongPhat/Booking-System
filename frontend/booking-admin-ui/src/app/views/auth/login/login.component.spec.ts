import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { Auth } from '../../../core/services/auth';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let auth: jasmine.SpyObj<Auth>;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<Auth>('Auth', ['loginWithKeycloak']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: Auth, useValue: auth },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({}),
            snapshot: {
              queryParamMap: {
                get: () => null,
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start keycloak login', () => {
    component.loginWithKeycloak();
    expect(auth.loginWithKeycloak).toHaveBeenCalled();
  });
});
